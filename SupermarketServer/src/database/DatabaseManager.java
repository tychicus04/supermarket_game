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

        String createMatchHistoryTable =
                "CREATE TABLE IF NOT EXISTS match_history (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "room_id TEXT NOT NULL," +
                        "player1 TEXT NOT NULL," +
                        "player2 TEXT NOT NULL," +
                        "player1_score INTEGER NOT NULL," +
                        "player2_score INTEGER NOT NULL," +
                        "winner TEXT," +
                        "match_result TEXT NOT NULL," +
                        "played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "FOREIGN KEY (player1) REFERENCES users(username)," +
                        "FOREIGN KEY (player2) REFERENCES users(username)" +
                        ")";

        String createIndexScore =
            "CREATE INDEX IF NOT EXISTS idx_scores_username ON scores(username)";

        String createIndexHighScore =
            "CREATE INDEX IF NOT EXISTS idx_scores_high ON scores(score DESC)";

        String createIndexFriends1 =
            "CREATE INDEX IF NOT EXISTS idx_friends_user1 ON friends(user1)";

        String createIndexFriends2 =
            "CREATE INDEX IF NOT EXISTS idx_friends_user2 ON friends(user2)";

        String createIndexMatchPlayer1 =
                "CREATE INDEX IF NOT EXISTS idx_match_player1 ON match_history(player1)";

        String createIndexMatchPlayer2 =
                "CREATE INDEX IF NOT EXISTS idx_match_player2 ON match_history(player2)";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createScoresTable);
            stmt.execute(createFriendRequestsTable);
            stmt.execute(createFriendsTable);
            stmt.execute(createMatchHistoryTable);
            stmt.execute(createIndexScore);
            stmt.execute(createIndexHighScore);
            stmt.execute(createIndexFriends1);
            stmt.execute(createIndexFriends2);
            stmt.execute(createIndexMatchPlayer1);
            stmt.execute(createIndexMatchPlayer2);
            System.out.println("Database tables ready");
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
            System.err.println("Login error: " + e.getMessage());
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
                return false;
            }
            System.err.println("Registration error: " + e.getMessage());
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
            System.err.println("Save score error: " + e.getMessage());
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

    /**
     * Save match history to database
     */
    public boolean saveMatchHistory(String roomId, String player1, String player2,
                                    int player1Score, int player2Score) {
        String winner = null;
        String matchResult;

        if (player1Score > player2Score) {
            winner = player1;
            matchResult = "WIN";
        } else if (player1Score < player2Score) {
            winner = player2;
            matchResult = "LOSE";
        } else {
            matchResult = "DRAW";
        }

        String query = "INSERT INTO match_history (room_id, player1, player2, player1_score, player2_score, winner, match_result) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, roomId);
            pstmt.setString(2, player1);
            pstmt.setString(3, player2);
            pstmt.setInt(4, player1Score);
            pstmt.setInt(5, player2Score);
            pstmt.setString(6, winner);
            pstmt.setString(7, matchResult);
            pstmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            System.err.println("Save match history error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get match history for a specific user
     */
    public String getMatchHistory(String username, int limit) {
        String query =
                "SELECT room_id, player1, player2, player1_score, player2_score, winner, match_result, played_at " +
                        "FROM match_history " +
                        "WHERE player1 = ? OR player2 = ? " +
                        "ORDER BY played_at DESC " +
                        "LIMIT ?";

        StringBuilder sb = new StringBuilder();

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            pstmt.setInt(3, limit);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String roomId = rs.getString("room_id");
                    String player1 = rs.getString("player1");
                    String player2 = rs.getString("player2");
                    int player1Score = rs.getInt("player1_score");
                    int player2Score = rs.getInt("player2_score");
                    String winner = rs.getString("winner");
                    String matchResult = rs.getString("match_result");
                    String playedAt = rs.getString("played_at");

                    // Determine opponent and result from user's perspective
                    String opponent = player1.equals(username) ? player2 : player1;
                    int myScore = player1.equals(username) ? player1Score : player2Score;
                    int opponentScore = player1.equals(username) ? player2Score : player1Score;

                    String result;
                    if (winner == null) {
                        result = "DRAW";
                    } else if (winner.equals(username)) {
                        result = "WIN";
                    } else {
                        result = "LOSE";
                    }

                    // Format: result|opponent|myScore|opponentScore|playedAt
                    sb.append(result).append("|")
                            .append(opponent).append("|")
                            .append(myScore).append("|")
                            .append(opponentScore).append("|")
                            .append(playedAt).append("\n");
                }
            }
        } catch (SQLException e) {
            System.err.println("Get match history error: " + e.getMessage());
        }

        return sb.toString();
    }

    /**
     * Get match statistics for a user
     */
    public Map<String, Integer> getMatchStats(String username) {
        Map<String, Integer> stats = new HashMap<>();
        stats.put("wins", 0);
        stats.put("losses", 0);
        stats.put("draws", 0);
        stats.put("total_matches", 0);

        String query =
                "SELECT " +
                        "SUM(CASE WHEN winner = ? THEN 1 ELSE 0 END) as wins, " +
                        "SUM(CASE WHEN winner IS NOT NULL AND winner != ? THEN 1 ELSE 0 END) as losses, " +
                        "SUM(CASE WHEN winner IS NULL THEN 1 ELSE 0 END) as draws, " +
                        "COUNT(*) as total_matches " +
                        "FROM match_history " +
                        "WHERE player1 = ? OR player2 = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(query)) {
            pstmt.setString(1, username);
            pstmt.setString(2, username);
            pstmt.setString(3, username);
            pstmt.setString(4, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    stats.put("wins", rs.getInt("wins"));
                    stats.put("losses", rs.getInt("losses"));
                    stats.put("draws", rs.getInt("draws"));
                    stats.put("total_matches", rs.getInt("total_matches"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Get match stats error: " + e.getMessage());
        }

        return stats;
    }
}