package models;

import java.util.Random;
import java.util.UUID;

/**
 * SpawnedItem model - represents an item spawned in the game
 * Server-side authoritative model
 */
public class SpawnedItem {
    private final String itemID;
    private String itemName;
    private double timeRemaining;
    private double x; // Position X (0.0 to 1.0 normalized)
    private double y; // Position Y (0.0 to 1.0 normalized)

    private static final Random random = new Random();

    // Available items in the game
    public static final String[] AVAILABLE_ITEMS = {
            "MILK", "BREAD", "APPLE", "CARROT",
            "ORANGE", "EGGS", "CHEESE", "MEAT", "SODA"
    };

    public SpawnedItem(String itemName, double timeRemaining) {
        this.itemID = UUID.randomUUID().toString();
        this.itemName = itemName;
        this.timeRemaining = timeRemaining;
        this.x = randomPosition();
        this.y = randomPosition();
    }

    public SpawnedItem(String itemName, double timeRemaining, double x, double y) {
        this.itemID = UUID.randomUUID().toString();
        this.itemName = itemName;
        this.timeRemaining = timeRemaining;
        this.x = x;
        this.y = y;
    }

    /**
     * Generate random position (0.15 to 0.85 to avoid edges)
     */
    private static double randomPosition() {
        return 0.15 + (random.nextDouble() * 0.7);
    }

    /**
     * Update item's remaining time
     * @param deltaTime time in seconds to subtract
     * @return true if item expired
     */
    public boolean updateTime(double deltaTime) {
        timeRemaining -= deltaTime;
        return timeRemaining <= 0;
    }

    /**
     * Get random item name from available items
     */
    public static String getRandomItemName() {
        return AVAILABLE_ITEMS[random.nextInt(AVAILABLE_ITEMS.length)];
    }

    /**
     * Convert to JSON string for network transmission
     */
    public String toJson() {
        return String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"timeRemaining\":%.2f,\"x\":%.3f,\"y\":%.3f}",
                itemID, itemName, timeRemaining, x, y
        );
    }

    // Getters and Setters
    public String getItemID() {
        return itemID;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public double getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(double timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("Item[%s: %s at (%.2f, %.2f), %.1fs left]",
                itemID.substring(0, 8), itemName, x, y, timeRemaining);
    }
}