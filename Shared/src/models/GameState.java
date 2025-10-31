package models;

import java.util.List;
import java.util.Map;

/**
 * GameState - Complete game state for synchronization
 * Used to serialize and send game state from server to clients
 */
public class GameState {
    private int gameTimeRemaining;
    private List<Customer> customers;
    private List<SpawnedItem> items;
    private Map<String, Integer> scores;
    private Map<String, Integer> combos;

    public GameState(int gameTimeRemaining,
                     List<Customer> customers,
                     List<SpawnedItem> items,
                     Map<String, Integer> scores,
                     Map<String, Integer> combos) {
        this.gameTimeRemaining = gameTimeRemaining;
        this.customers = customers;
        this.items = items;
        this.scores = scores;
        this.combos = combos;
    }

    /**
     * Convert entire game state to JSON string
     * Format optimized for network transmission
     */
    public String toJson() {
        StringBuilder json = new StringBuilder();
        json.append("{");

        // Time remaining
        json.append("\"timeRemaining\":").append(gameTimeRemaining).append(",");

        // Customers array
        json.append("\"customers\":[");
        for (int i = 0; i < customers.size(); i++) {
            json.append(customers.get(i).toJson());
            if (i < customers.size() - 1) json.append(",");
        }
        json.append("],");

        // Items array
        json.append("\"items\":[");
        for (int i = 0; i < items.size(); i++) {
            json.append(items.get(i).toJson());
            if (i < items.size() - 1) json.append(",");
        }
        json.append("],");

        // Scores object
        json.append("\"scores\":{");
        int scoreCount = 0;
        for (Map.Entry<String, Integer> entry : scores.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            if (++scoreCount < scores.size()) json.append(",");
        }
        json.append("},");

        // Combos object
        json.append("\"combos\":{");
        int comboCount = 0;
        for (Map.Entry<String, Integer> entry : combos.entrySet()) {
            json.append("\"").append(entry.getKey()).append("\":").append(entry.getValue());
            if (++comboCount < combos.size()) json.append(",");
        }
        json.append("}");

        json.append("}");
        return json.toString();
    }

    // Getters
    public int getGameTimeRemaining() {
        return gameTimeRemaining;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public List<SpawnedItem> getItems() {
        return items;
    }

    public Map<String, Integer> getScores() {
        return scores;
    }

    public Map<String, Integer> getCombos() {
        return combos;
    }

    @Override
    public String toString() {
        return String.format("GameState[time=%ds, customers=%d, items=%d, players=%d]",
                gameTimeRemaining, customers.size(), items.size(), scores.size());
    }
}