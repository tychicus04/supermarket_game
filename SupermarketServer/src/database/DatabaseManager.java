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
        
        String createIndexScore = 
            "CREATE INDEX IF NOT EXISTS idx_scores_username ON scores(username)";
        
        String createIndexHighScore = 
            "CREATE INDEX IF NOT EXISTS idx_scores_high ON scores(score DESC)";
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createUsersTable);
            stmt.execute(createScoresTable);
            stmt.execute(createIndexScore);
            stmt.execute(createIndexHighScore);
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