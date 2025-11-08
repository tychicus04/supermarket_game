import database.DatabaseManager;

/**
 * Initialize the database by creating all tables
 * Run this once to set up your database schema
 */
public class InitializeDatabase {
    public static void main(String[] args) {
        System.out.println("=== Database Initialization ===\n");

        DatabaseManager dbManager = new DatabaseManager();

        if (dbManager.initialize()) {
            System.out.println("\n[OK] Database initialized successfully!");
            System.out.println("Database file: supermarket_game.db");
            System.out.println("\nYou can now run the server or test programs.");

            // Show some stats
            int totalUsers = dbManager.getTotalUsers();
            int totalGames = dbManager.getTotalGames();

            System.out.println("\nCurrent Statistics:");
            System.out.println("   Users: " + totalUsers);
            System.out.println("   Games played: " + totalGames);

            dbManager.close();
        } else {
            System.err.println("\n[ERROR] Database initialization failed!");
            System.err.println("Check the error messages above.");
            System.exit(1);
        }
    }
}

