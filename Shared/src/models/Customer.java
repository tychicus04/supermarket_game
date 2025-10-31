package models;

import java.util.UUID;

/**
 * Customer model - represents a customer waiting for items
 * Server-side authoritative model
 */
public class Customer {
    private final String customerID;
    private String requestItemName;
    private double timeRemaining;
    private double timeMax;
    private String mood; // "neutral", "happy", "angry"
    private int slotIndex; // Position slot (0, 1, or 2)

    public Customer(String requestItemName, double timeMax, int slotIndex) {
        this.customerID = UUID.randomUUID().toString();
        this.requestItemName = requestItemName;
        this.timeMax = timeMax;
        this.timeRemaining = timeMax;
        this.mood = "neutral";
        this.slotIndex = slotIndex;
    }

    /**
     * Update customer's remaining time
     * @param deltaTime time in seconds to subtract
     * @return true if customer timed out
     */
    public boolean updateTime(double deltaTime) {
        timeRemaining -= deltaTime;
        return timeRemaining <= 0;
    }

    /**
     * Get progress as percentage (0.0 to 1.0)
     */
    public double getProgress() {
        return Math.max(0, Math.min(1.0, timeRemaining / timeMax));
    }

    /**
     * Check if the item matches customer's request
     */
    public boolean wantsItem(String itemName) {
        return requestItemName.equals(itemName);
    }

    /**
     * Convert to JSON string for network transmission
     */
    public String toJson() {
        return String.format(
                "{\"id\":\"%s\",\"item\":\"%s\",\"timeRemaining\":%.2f,\"timeMax\":%.2f,\"mood\":\"%s\",\"slot\":%d}",
                customerID, requestItemName, timeRemaining, timeMax, mood, slotIndex
        );
    }

    // Getters and Setters
    public String getCustomerID() {
        return customerID;
    }

    public String getRequestItemName() {
        return requestItemName;
    }

    public void setRequestItemName(String requestItemName) {
        this.requestItemName = requestItemName;
    }

    public double getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(double timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    public double getTimeMax() {
        return timeMax;
    }

    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public void setSlotIndex(int slotIndex) {
        this.slotIndex = slotIndex;
    }

    @Override
    public String toString() {
        return String.format("Customer[%s wants %s, %.1fs left, slot %d, %s]",
                customerID.substring(0, 8), requestItemName, timeRemaining, slotIndex, mood);
    }
}