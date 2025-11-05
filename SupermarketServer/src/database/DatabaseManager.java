package database;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:supermarket_game.db";
    private Connection connection;
    
    /**
     * Initialize database and create tables
     */
    public boolean initialize() {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            
            // Connect to database (creates file if not exists)
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
     * Create database tables if not exist
     */
    private void createTables() throws SQLException {
        String createUsersTable = 
            "CREATE TABLE IF NOT EXISTS users (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "username TEXT UNIQUE NOT NULL," +
            "password TEXT NOT NULL," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")";
        
        String createScoresTable = 
            "CREATE TABLE IF NOT EXISTS scores (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "username TEXT NOT NULL," +
            "score INTEGER NOT NULL," +
            "played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "FOREIGN KEY (username) REFERENCES users(username)" +
            ")";
        
        String createFriendRequestsTable =
            "CREATE TABLE IF NOT EXISTS friend_requests (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "from_username TEXT NOT NULL," +
            "to_username TEXT NOT NULL," +
            "status TEXT DEFAULT 'pending'," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "FOREIGN KEY (from_username) REFERENCES users(username)," +
            "FOREIGN KEY (to_username) REFERENCES users(username)," +
            "UNIQUE(from_username, to_username)" +
            ")";

        String createFriendsTable =
            "CREATE TABLE IF NOT EXISTS friends (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "user1 TEXT NOT NULL," +
            "user2 TEXT NOT NULL," +
            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
            "FOREIGN KEY (user1) REFERENCES users(username)," +
            "FOREIGN KEY (user2) REFERENCES users(username)," +
            "UNIQUE(user1, user2)" +
            ")";

        String createIndexScore =
            "CREATE INDEX IF NOT EXISTS idx_scores_username ON scores(username)";
        
        String createIndexHighScore = 
            "CREATE INDEX IF NOT EXISTS idx_scores_high ON scores(score DESC)";
        
        String createIndexFriends1 =
            "CREATE INDEX IF NOT EXISTS idx_friends_user1 ON friends(user1)";

        String createIndexFriends2 =
            "CREATE INDEX IF NOT EXISTS idx_friends_user2 ON friends(user2)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createScoresTable);
            stmt.execute(createFriendRequestsTable);
            stmt.execute(createFriendsTable);
            stmt.execute(createIndexScore);
            stmt.execute(createIndexHighScore);
            stmt.execute(createIndexFriends1);
            stmt.execute(createIndexFriends2);
            System.out.println("✅ Database tables ready");
        }
    }
    
    /**
     * Validate user credentials
     */
    public boolean validateUser(String username, String password) {
        String query = "SELECT password FROM users WHERE username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    return storedPassword.equals(password);
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
        String query = "INSERT INTO users (username, password) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            if (e.getMessage().contains("UNIQUE constraint failed")) {
                return false; // Username exists
            }
            System.err.println("❌ Registration error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Save game score
     */
    public boolean saveScore(String username, int score) {
        String query = "INSERT INTO scores (username, score) VALUES (?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setInt(2, score);
            pstmt.executeUpdate();
            return true;
            
        } catch (SQLException e) {
            System.err.println("❌ Save score error: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get leaderboard (top N players by highest score)
     */
    public String getLeaderboard(int limit) {
        String query = 
            "SELECT username, MAX(score) as high_score " +
            "FROM scores " +
            "GROUP BY username " +
            "ORDER BY high_score DESC " +
            "LIMIT ?";
        
        StringBuilder sb = new StringBuilder();
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setInt(1, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    String username = rs.getString("username");
                    int score = rs.getInt("high_score");
                    
                    sb.append(rank).append(".")
                      .append(username).append(":")
                      .append(score).append("\n");
                    rank++;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Leaderboard error: " + e.getMessage());
        }
        
        return sb.toString();
    }
    
    /**
     * Get player statistics
     */
    public Map<String, Object> getPlayerStats(String username) {
        Map<String, Object> stats = new HashMap<>();
        
        String query = 
            "SELECT " +
            "COUNT(*) as games_played, " +
            "MAX(score) as high_score, " +
            "AVG(score) as avg_score, " +
            "SUM(score) as total_score " +
            "FROM scores " +
            "WHERE username = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("games_played", rs.getInt("games_played"));
                    stats.put("high_score", rs.getInt("high_score"));
                    stats.put("avg_score", rs.getDouble("avg_score"));
                    stats.put("total_score", rs.getInt("total_score"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Stats error: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Get player's recent games
     */
    public List<Map<String, Object>> getRecentGames(String username, int limit) {
        List<Map<String, Object>> games = new ArrayList<>();
        
        String query = 
            "SELECT score, played_at " +
            "FROM scores " +
            "WHERE username = ? " +
            "ORDER BY played_at DESC " +
            "LIMIT ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setInt(2, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> game = new HashMap<>();
                    game.put("score", rs.getInt("score"));
                    game.put("played_at", rs.getTimestamp("played_at"));
                    games.add(game);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Recent games error: " + e.getMessage());
        }
        
        return games;
    }
    
    // ==================== FRIEND MANAGEMENT ====================

    /**
     * Search users by username (for adding friends)
     */
    public List<String> searchUsers(String searchTerm, String excludeUser) {
        List<String> users = new ArrayList<>();
        String query = "SELECT username FROM users WHERE username LIKE ? AND username != ? LIMIT 20";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, "%" + searchTerm + "%");
            pstmt.setString(2, excludeUser);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Search users error: " + e.getMessage());
        }

        return users;
    }

    /**
     * Send friend request
     */
    public boolean sendFriendRequest(String fromUser, String toUser) {
        // Check if already friends
        if (areFriends(fromUser, toUser)) {
            return false;
        }

        // Check if request already exists
        String checkQuery = "SELECT * FROM friend_requests WHERE " +
                           "(from_username = ? AND to_username = ?) OR " +
                           "(from_username = ? AND to_username = ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(checkQuery)) {
            pstmt.setString(1, fromUser);
            pstmt.setString(2, toUser);
            pstmt.setString(3, toUser);
            pstmt.setString(4, fromUser);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return false; // Request already exists
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Check friend request error: " + e.getMessage());
            return false;
        }

        // Insert new request
        String query = "INSERT INTO friend_requests (from_username, to_username, status) VALUES (?, ?, 'pending')";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, fromUser);
            pstmt.setString(2, toUser);
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
    public boolean acceptFriendRequest(String fromUser, String toUser) {
        try {
            connection.setAutoCommit(false);

            // Update request status
            String updateQuery = "UPDATE friend_requests SET status = 'accepted' " +
                                "WHERE from_username = ? AND to_username = ? AND status = 'pending'";

            try (PreparedStatement pstmt = connection.prepareStatement(updateQuery)) {
                pstmt.setString(1, fromUser);
                pstmt.setString(2, toUser);
                int updated = pstmt.executeUpdate();

                if (updated == 0) {
                    connection.rollback();
                    return false;
                }
            }

            // Add to friends table (ensure user1 < user2 for consistency)
            String user1 = fromUser.compareTo(toUser) < 0 ? fromUser : toUser;
            String user2 = fromUser.compareTo(toUser) < 0 ? toUser : fromUser;

            String insertQuery = "INSERT INTO friends (user1, user2) VALUES (?, ?)";

            try (PreparedStatement pstmt = connection.prepareStatement(insertQuery)) {
                pstmt.setString(1, user1);
                pstmt.setString(2, user2);
                pstmt.executeUpdate();
            }

            connection.commit();
            connection.setAutoCommit(true);
            return true;

        } catch (SQLException e) {
            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            System.err.println("❌ Accept friend request error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reject friend request
     */
    public boolean rejectFriendRequest(String fromUser, String toUser) {
        String query = "UPDATE friend_requests SET status = 'rejected' " +
                      "WHERE from_username = ? AND to_username = ? AND status = 'pending'";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, fromUser);
            pstmt.setString(2, toUser);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Reject friend request error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get pending friend requests for a user
     */
    public List<String> getPendingFriendRequests(String username) {
        List<String> requests = new ArrayList<>();
        String query = "SELECT from_username FROM friend_requests " +
                      "WHERE to_username = ? AND status = 'pending' " +
                      "ORDER BY created_at DESC";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    requests.add(rs.getString("from_username"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Get pending requests error: " + e.getMessage());
        }

        return requests;
    }

    /**
     * Get friends list for a user
     */
    public List<String> getFriends(String username) {
        List<String> friends = new ArrayList<>();
        String query = "SELECT CASE " +
                      "WHEN user1 = ? THEN user2 " +
                      "WHEN user2 = ? THEN user1 " +
                      "END as friend " +
                      "FROM friends " +
                      "WHERE user1 = ? OR user2 = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            pstmt.setString(3, username);
            pstmt.setString(4, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    friends.add(rs.getString("friend"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Get friends error: " + e.getMessage());
        }

        return friends;
    }

    /**
     * Check if two users are friends
     */
    public boolean areFriends(String user1, String user2) {
        String query = "SELECT * FROM friends WHERE " +
                      "(user1 = ? AND user2 = ?) OR (user1 = ? AND user2 = ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            String u1 = user1.compareTo(user2) < 0 ? user1 : user2;
            String u2 = user1.compareTo(user2) < 0 ? user2 : user1;

            pstmt.setString(1, u1);
            pstmt.setString(2, u2);
            pstmt.setString(3, u2);
            pstmt.setString(4, u1);

            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("❌ Check friends error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Remove friend
     */
    public boolean removeFriend(String user1, String user2) {
        String query = "DELETE FROM friends WHERE " +
                      "(user1 = ? AND user2 = ?) OR (user1 = ? AND user2 = ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            String u1 = user1.compareTo(user2) < 0 ? user1 : user2;
            String u2 = user1.compareTo(user2) < 0 ? user2 : user1;

            pstmt.setString(1, u1);
            pstmt.setString(2, u2);
            pstmt.setString(3, u2);
            pstmt.setString(4, u1);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Remove friend error: " + e.getMessage());
            return false;
        }
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
    
    /**
     * Get total number of registered users
     */
    public int getTotalUsers() {
        String query = "SELECT COUNT(*) as total FROM users";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("❌ Count users error: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Get total number of games played
     */
    public int getTotalGames() {
        String query = "SELECT COUNT(*) as total FROM scores";
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            System.err.println("❌ Count games error: " + e.getMessage());
        }
        
        return 0;
    }
}