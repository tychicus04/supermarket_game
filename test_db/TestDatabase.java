import java.sql.*;

/**
 * Simple test program to verify database connection and operations
 */
public class TestDatabase {
    private static final String DB_URL = "jdbc:sqlite:data/supermarket_game.db";
    
    public static void main(String[] args) {
        System.out.println("=== Database Connection Test ===\n");
        
        try {
            // Load driver
            Class.forName("org.sqlite.JDBC");
            System.out.println("[OK] SQLite JDBC driver loaded");
            
            // Connect to database
            Connection conn = DriverManager.getConnection(DB_URL);
            System.out.println("[OK] Connected to: " + DB_URL);
            
            // Check if tables exist
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet tables = meta.getTables(null, null, "users", null);
            if (tables.next()) {
                System.out.println("[OK] Table 'users' exists");
            } else {
                System.out.println("[ERROR] Table 'users' does NOT exist");
            }
            tables.close();
            
            // Count users
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as cnt FROM users");
            if (rs.next()) {
                int count = rs.getInt("cnt");
                System.out.println("[INFO] Total users: " + count);
            }
            rs.close();
            
            // List all users
            System.out.println("\n=== Current Users ===");
            rs = stmt.executeQuery("SELECT id, username, created_at FROM users ORDER BY created_at DESC");
            boolean hasUsers = false;
            while (rs.next()) {
                hasUsers = true;
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String created = rs.getString("created_at");
                System.out.println("  [" + id + "] " + username + " (created: " + created + ")");
            }
            if (!hasUsers) {
                System.out.println("  (No users found)");
            }
            rs.close();
            
            // Test insert (with rollback)
            System.out.println("\n=== Testing Insert Operation ===");
            conn.setAutoCommit(false);
            
            PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO users (username, password) VALUES (?, ?)"
            );
            pstmt.setString(1, "test_user_" + System.currentTimeMillis());
            pstmt.setString(2, "test_password");
            
            int inserted = pstmt.executeUpdate();
            System.out.println("[OK] Insert test successful (rows affected: " + inserted + ")");
            
            // Rollback test insert
            conn.rollback();
            System.out.println("[OK] Transaction rolled back (test user not saved)");
            
            pstmt.close();
            stmt.close();
            conn.close();
            
            System.out.println("\n=== All Tests Passed ===");
            
        } catch (ClassNotFoundException e) {
            System.err.println("[ERROR] SQLite JDBC driver not found!");
            System.err.println("   Make sure sqlite-jdbc.jar is in classpath");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("[ERROR] Database error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
