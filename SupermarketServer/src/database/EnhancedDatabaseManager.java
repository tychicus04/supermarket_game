package database;

import java.sql.*;

/**
 * Enhanced DatabaseManager with friends and match history support
 * Extends the original database functionality
 */
public class EnhancedDatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:supermarket_game.db";
    private Connection connection;

    /**
     * Initialize database and create all tables
     */
    public boolean initialize() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);

            System.out.println("📁 Database: supermarket_game.db");

            createTables();
            return true;

        } catch (ClassNotFoundException e) {
            System.err.println("❌ SQLite JDBC driver not found!");
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Database error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Create all database tables
     */
    private void createTables() throws SQLException {
        // Users table (with user_id as PRIMARY KEY for friendships)
        String createUsersTable =
                "CREATE TABLE IF NOT EXISTS users (" +
                        "user_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "username TEXT UNIQUE NOT NULL," +
                        "password_hash TEXT NOT NULL," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")";

        // Scores table
        String createScoresTable =
                "CREATE TABLE IF NOT EXISTS scores (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "user_id INTEGER NOT NULL," +
                        "score INTEGER NOT NULL," +
                        "played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                        ")";

        // Friends table (NEW)
        String createFriendsTable =
                "CREATE TABLE IF NOT EXISTS friends (" +
                        "friendship_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "user_id_1 INTEGER NOT NULL," +
                        "user_id_2 INTEGER NOT NULL," +
                        "status TEXT NOT NULL CHECK(status IN ('pending', 'accepted'))," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (user_id_1) REFERENCES users(user_id)," +
                        "FOREIGN KEY (user_id_2) REFERENCES users(user_id)," +
                        "UNIQUE(user_id_1, user_id_2)" +
                        ")";

        // Match history table (NEW)
        String createMatchHistoryTable =
                "CREATE TABLE IF NOT EXISTS match_history (" +
                        "match_id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "room_id TEXT NOT NULL," +
                        "user_id INTEGER NOT NULL," +
                        "score INTEGER NOT NULL," +
                        "rank INTEGER NOT NULL," +
                        "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (user_id) REFERENCES users(user_id)" +
                        ")";

        // Indexes for performance
        String[] indexes = {
                "CREATE INDEX IF NOT EXISTS idx_scores_user ON scores(user_id)",
                "CREATE INDEX IF NOT EXISTS idx_friends_user1 ON friends(user_id_1)",
                "CREATE INDEX IF NOT EXISTS idx_friends_user2 ON friends(user_id_2)",
                "CREATE INDEX IF NOT EXISTS idx_match_history_user ON match_history(user_id)",
                "CREATE INDEX IF NOT EXISTS idx_match_history_room ON match_history(room_id)"
        };

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createScoresTable);
            stmt.execute(createFriendsTable);
            stmt.execute(createMatchHistoryTable);

            for (String index : indexes) {
                stmt.execute(index);
            }

            System.out.println("✅ Database tables ready (with friends & match_history)");
        }
    }

    // ==================== USER MANAGEMENT ====================

    /**
     * Validate user credentials
     */
    public boolean validateUser(String username, String password) {
        String query = "SELECT password_hash FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("password_hash").equals(password);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Login error: " + e.getMessage());
        }

        return false;
    }

    /**
     * Register new user
     */
    public boolean registerUser(String username, String password) {
        String query = "INSERT INTO users (username, password_hash) VALUES (?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                return false;
            }
            System.err.println("❌ Registration error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get user ID by username
     */
    public int getUserId(String username) {
        String query = "SELECT user_id FROM users WHERE username = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Get user ID error: " + e.getMessage());
        }

        return -1;
    }

    // ==================== FRIEND MANAGEMENT ====================

    /**
     * Send friend request
     */
    public boolean sendFriendRequest(String fromUsername, String toUsername) {
        int fromId = getUserId(fromUsername);
        int toId = getUserId(toUsername);

        if (fromId == -1 || toId == -1) {
            return false;
        }

        String query = "INSERT INTO friends (user_id_1, user_id_2, status) VALUES (?, ?, 'pending')";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, fromId);
            pstmt.setInt(2, toId);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Send friend request error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Accept friend request
     */
    public boolean acceptFriendRequest(String accepterUsername, String requesterUsername) {
        int accepterId = getUserId(accepterUsername);
        int requesterId = getUserId(requesterUsername);

        if (accepterId == -1 || requesterId == -1) {
            return false;
        }

        String query = "UPDATE friends SET status = 'accepted' " +
                "WHERE user_id_1 = ? AND user_id_2 = ? AND status = 'pending'";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, requesterId);
            pstmt.setInt(2, accepterId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ Accept friend request error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get friend list (accepted friends)
     */
    public String getFriendListJson(String username) {
        int userId = getUserId(username);
        if (userId == -1) return "[]";

        String query =
                "SELECT u.username, u.user_id FROM users u " +
                        "INNER JOIN friends f ON (f.user_id_1 = ? AND f.user_id_2 = u.user_id) " +
                        "                     OR (f.user_id_2 = ? AND f.user_id_1 = u.user_id) " +
                        "WHERE f.status = 'accepted'";

        StringBuilder json = new StringBuilder("[");

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{\"username\":\"").append(rs.getString("username"))
                            .append("\",\"user_id\":").append(rs.getInt("user_id"))
                            .append("}");
                    first = false;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Get friend list error: " + e.getMessage());
        }

        json.append("]");
        return json.toString();
    }

    // ==================== MATCH HISTORY ====================

    /**
     * Save match result for a player
     */
    public boolean saveMatchResult(String roomId, String username, int score, int rank) {
        int userId = getUserId(username);
        if (userId == -1) return false;

        String query = "INSERT INTO match_history (room_id, user_id, score, rank) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, roomId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, score);
            pstmt.setInt(4, rank);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Save match result error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get match history for a user
     */
    public String getMatchHistoryJson(String username, int limit) {
        int userId = getUserId(username);
        if (userId == -1) return "[]";

        String query =
                "SELECT room_id, score, rank, timestamp " +
                        "FROM match_history " +
                        "WHERE user_id = ? " +
                        "ORDER BY timestamp DESC " +
                        "LIMIT ?";

        StringBuilder json = new StringBuilder("[");

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                boolean first = true;
                while (rs.next()) {
                    if (!first) json.append(",");
                    json.append("{\"room\":\"").append(rs.getString("room_id"))
                            .append("\",\"score\":").append(rs.getInt("score"))
                            .append(",\"rank\":").append(rs.getInt("rank"))
                            .append(",\"time\":\"").append(rs.getTimestamp("timestamp"))
                            .append("\"}");
                    first = false;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Get match history error: " + e.getMessage());
        }

        json.append("]");
        return json.toString();
    }

    // ==================== LEADERBOARD ====================

    /**
     * Get leaderboard (top N players by highest score)
     */
    public String getLeaderboard(int limit) {
        String query =
                "SELECT u.username, MAX(s.score) as high_score " +
                        "FROM users u " +
                        "INNER JOIN scores s ON u.user_id = s.user_id " +
                        "GROUP BY u.user_id " +
                        "ORDER BY high_score DESC " +
                        "LIMIT ?";

        StringBuilder sb = new StringBuilder();

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    sb.append(rank++).append(".")
                            .append(rs.getString("username")).append(":")
                            .append(rs.getInt("high_score")).append("\n");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Leaderboard error: " + e.getMessage());
        }

        return sb.toString();
    }

    /**
     * Close database connection
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("📁 Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error closing database: " + e.getMessage());
        }
    }
}