package utils;

import models.Message;

/**
 * Centralized error handling for server-side operations.
 * Provides consistent error responses and logging.
 */
public class ServerErrorHandler {

    /**
     * Handle authentication errors
     */
    public static Message handleAuthError(String username, String operation, Exception e) {
        String message = username != null ?
            "Authentication failed for user: " + username :
            "Authentication failed: Not logged in";

        logError(operation, message, e);
        return new Message("ERROR", "Authentication required");
    }

    /**
     * Handle validation errors
     */
    public static Message handleValidationError(String field, String reason) {
        logError("VALIDATION", field + ": " + reason, null);
        return new Message("ERROR", reason);
    }

    /**
     * Handle room operation errors
     */
    public static Message handleRoomError(String roomId, String operation, String reason) {
        logError("ROOM_" + operation.toUpperCase(),
                "Room " + roomId + ": " + reason, null);
        return new Message("ERROR", reason);
    }

    /**
     * Handle database errors
     */
    public static Message handleDatabaseError(String operation, Exception e) {
        logError("DATABASE_" + operation.toUpperCase(),
                "Database operation failed", e);
        return new Message("ERROR", "Server error. Please try again.");
    }

    /**
     * Handle network errors
     */
    public static Message handleNetworkError(String username, Exception e) {
        logError("NETWORK",
                "Network error for client " + (username != null ? username : "unknown"),
                e);
        return new Message("ERROR", "Connection error");
    }

    /**
     * Handle general errors with custom message
     */
    public static Message handleError(String operation, String message) {
        logError(operation, message, null);
        return new Message("ERROR", message);
    }

    /**
     * Handle general errors with exception
     */
    public static Message handleError(String operation, String message, Exception e) {
        logError(operation, message, e);
        return new Message("ERROR", message);
    }

    /**
     * Log error with consistent format
     */
    private static void logError(String operation, String message, Exception e) {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date());

        System.err.println("[" + timestamp + "] [ERROR] [" + operation + "] " + message);

        if (e != null) {
            System.err.println("  Exception: " + e.getClass().getName() +
                             ": " + e.getMessage());
            // Print first 3 stack trace elements for debugging
            StackTraceElement[] stackTrace = e.getStackTrace();
            for (int i = 0; i < Math.min(3, stackTrace.length); i++) {
                System.err.println("    at " + stackTrace[i]);
            }
        }
    }

    /**
     * Log warning
     */
    public static void logWarning(String operation, String message) {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date());

        System.out.println("[" + timestamp + "] [WARN] [" + operation + "] " + message);
    }

    /**
     * Log info
     */
    public static void logInfo(String operation, String message) {
        String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
            .format(new java.util.Date());

        System.out.println("[" + timestamp + "] [INFO] [" + operation + "] " + message);
    }

    /**
     * Validate string input
     */
    public static boolean validateString(String value, int minLength, int maxLength) {
        return value != null &&
               value.trim().length() >= minLength &&
               value.trim().length() <= maxLength;
    }

    /**
     * Validate username format
     */
    public static String validateUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return "Username cannot be empty";
        }

        username = username.trim();

        if (username.length() < 3) {
            return "Username must be at least 3 characters";
        }

        if (username.length() > 20) {
            return "Username must be at most 20 characters";
        }

        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            return "Username can only contain letters, numbers, and underscores";
        }

        return null; // Valid
    }

    /**
     * Validate password format
     */
    public static String validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return "Password cannot be empty";
        }

        if (password.length() < 4) {
            return "Password must be at least 4 characters";
        }

        if (password.length() > 50) {
            return "Password must be at most 50 characters";
        }

        return null; // Valid
    }
}

