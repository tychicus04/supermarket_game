package utils;

import java.util.List;
import java.util.Map;

/**
 * Utility class for building JSON strings without external dependencies.
 * Eliminates code duplication across the server.
 */
public class JsonBuilder {

    /**
     * Build a JSON array from a list of strings
     * Example: ["item1", "item2", "item3"]
     */
    public static String buildStringArray(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(escapeJson(items.get(i))).append("\"");
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Build a JSON array of objects from a list of maps
     * Example: [{"key1":"value1","key2":"value2"},...]
     */
    public static String buildObjectArray(List<Map<String, Object>> objects) {
        if (objects == null || objects.isEmpty()) {
            return "[]";
        }

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < objects.size(); i++) {
            if (i > 0) json.append(",");
            json.append(buildObject(objects.get(i)));
        }
        json.append("]");
        return json.toString();
    }

    /**
     * Build a JSON object from a map
     * Example: {"key1":"value1","key2":123,"key3":true}
     */
    public static String buildObject(Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return "{}";
        }

        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            if (!first) json.append(",");
            first = false;

            json.append("\"").append(escapeJson(entry.getKey())).append("\":");
            json.append(formatValue(entry.getValue()));
        }

        json.append("}");
        return json.toString();
    }

    /**
     * Build a friend object with username and online status
     * Example: {"username":"player1","online":true}
     */
    public static String buildFriendObject(String username, boolean isOnline) {
        return "{\"username\":\"" + escapeJson(username) +
               "\",\"online\":" + isOnline + "}";
    }

    /**
     * Build a room object
     * Example: {"roomId":"ROOM123","creator":"player1","playerCount":2,"maxPlayers":4}
     */
    public static String buildRoomObject(String roomId, String creator, int playerCount, int maxPlayers) {
        return "{\"roomId\":\"" + escapeJson(roomId) +
               "\",\"creator\":\"" + escapeJson(creator) +
               "\",\"playerCount\":" + playerCount +
               ",\"maxPlayers\":" + maxPlayers + "}";
    }

    /**
     * Build a room update object with creator and players list
     * Example: {"creator":"player1","players":["player1","player2"]}
     */
    public static String buildRoomUpdate(String creator, List<String> players) {
        StringBuilder json = new StringBuilder("{\"creator\":\"")
            .append(escapeJson(creator))
            .append("\",\"players\":[");

        for (int i = 0; i < players.size(); i++) {
            if (i > 0) json.append(",");
            json.append("\"").append(escapeJson(players.get(i))).append("\"");
        }

        json.append("]}");
        return json.toString();
    }

    /**
     * Format a value based on its type
     */
    private static String formatValue(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJson((String) value) + "\"";
        } else if (value instanceof Boolean || value instanceof Number) {
            return value.toString();
        } else if (value instanceof List) {
            return buildStringArray((List<String>) value);
        } else {
            return "\"" + escapeJson(value.toString()) + "\"";
        }
    }

    /**
     * Escape special characters in JSON strings
     */
    private static String escapeJson(String str) {
        if (str == null) return "";

        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Builder pattern for complex JSON objects
     */
    public static class ObjectBuilder {
        private final StringBuilder json = new StringBuilder("{");
        private boolean first = true;

        public ObjectBuilder add(String key, Object value) {
            if (!first) json.append(",");
            first = false;

            json.append("\"").append(escapeJson(key)).append("\":");
            json.append(formatValue(value));
            return this;
        }

        public ObjectBuilder addRaw(String key, String rawJsonValue) {
            if (!first) json.append(",");
            first = false;

            json.append("\"").append(escapeJson(key)).append("\":");
            json.append(rawJsonValue);
            return this;
        }

        public String build() {
            json.append("}");
            return json.toString();
        }
    }

    /**
     * Builder pattern for JSON arrays
     */
    public static class ArrayBuilder {
        private final StringBuilder json = new StringBuilder("[");
        private boolean first = true;

        public ArrayBuilder add(String value) {
            if (!first) json.append(",");
            first = false;
            json.append("\"").append(escapeJson(value)).append("\"");
            return this;
        }

        public ArrayBuilder addObject(String objectJson) {
            if (!first) json.append(",");
            first = false;
            json.append(objectJson);
            return this;
        }

        public String build() {
            json.append("]");
            return json.toString();
        }
    }
}

