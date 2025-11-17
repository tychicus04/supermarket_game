package utils;

/**
 * Utility class for parsing simple JSON strings without external dependencies.
 * Handles the common parsing patterns used in the client code.
 */
public class JsonParser {

    /**
     * Extract a string value from JSON
     * Example: extractString(json, "username") from {"username":"player1","score":100}
     */
    public static String extractString(String json, String key) {
        String searchKey = "\"" + key + "\":\"";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;

        start += searchKey.length();
        int end = json.indexOf("\"", start);
        if (end == -1) return null;

        return json.substring(start, end);
    }

    /**
     * Extract an integer value from JSON
     * Example: extractInt(json, "score") from {"username":"player1","score":100}
     */
    public static int extractInt(String json, String key, int defaultValue) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return defaultValue;

        start += searchKey.length();
        int end = findNumberEnd(json, start);
        if (end == -1) return defaultValue;

        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Extract a boolean value from JSON
     * Example: extractBoolean(json, "online") from {"username":"player1","online":true}
     */
    public static boolean extractBoolean(String json, String key, boolean defaultValue) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return defaultValue;

        start += searchKey.length();
        String remaining = json.substring(start).trim();

        if (remaining.startsWith("true")) return true;
        if (remaining.startsWith("false")) return false;

        return defaultValue;
    }

    /**
     * Extract an array from JSON
     * Example: extractArray(json, "players") from {"players":["p1","p2"]}
     * Returns the array content without brackets: "p1","p2"
     */
    public static String extractArray(String json, String key) {
        String searchKey = "\"" + key + "\":[";
        int start = json.indexOf(searchKey);
        if (start == -1) return null;

        start += searchKey.length();
        int end = json.indexOf("]", start);
        if (end == -1) return null;

        return json.substring(start, end);
    }

    /**
     * Split array items (handles quoted strings)
     * Example: ["item1","item2","item3"] -> ["item1", "item2", "item3"]
     */
    public static String[] splitArrayItems(String arrayContent) {
        if (arrayContent == null || arrayContent.trim().isEmpty()) {
            return new String[0];
        }

        return arrayContent.replace("\"", "").split(",");
    }

    /**
     * Check if JSON represents an empty array
     */
    public static boolean isEmptyArray(String json) {
        return json != null && json.trim().equals("[]");
    }

    /**
     * Check if JSON represents an empty object
     */
    public static boolean isEmptyObject(String json) {
        return json != null && json.trim().equals("{}");
    }

    /**
     * Split a JSON array into individual JSON objects
     * Example: [{"a":1},{"b":2}] -> ["{\"a\":1}", "{\"b\":2}"]
     */
    public static String[] splitJsonArray(String json) {
        if (isEmptyArray(json)) {
            return new String[0];
        }

        // Remove outer brackets
        json = json.trim();
        if (json.startsWith("[")) json = json.substring(1);
        if (json.endsWith("]")) json = json.substring(0, json.length() - 1);

        java.util.List<String> objects = new java.util.ArrayList<>();
        int braceCount = 0;
        int start = 0;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (braceCount == 0) start = i;
                braceCount++;
            }
            if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    objects.add(json.substring(start, i + 1));
                }
            }
        }

        return objects.toArray(new String[0]);
    }

    /**
     * Find the end position of a number in JSON
     */
    private static int findNumberEnd(String json, int start) {
        int i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == ',' || c == '}' || c == ']' || c == ' ') {
                return i;
            }
            i++;
        }
        return json.length();
    }

    /**
     * Unescape JSON string
     */
    public static String unescapeJson(String str) {
        if (str == null) return null;

        return str.replace("\\\\", "\\")
                  .replace("\\\"", "\"")
                  .replace("\\n", "\n")
                  .replace("\\r", "\r")
                  .replace("\\t", "\t");
    }
}

