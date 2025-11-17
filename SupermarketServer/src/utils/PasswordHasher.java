package utils;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utility class for password hashing using BCrypt
 */
public class PasswordHasher {

    // BCrypt work factor (number of iterations: 2^12 = 4096)
    private static final int BCRYPT_ROUNDS = 12;

    /**
     * Hash a password using BCrypt
     *
     * @param plainPassword The plain text password to hash
     * @return The hashed password
     */
    public static String hashPassword(String plainPassword) {
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(BCRYPT_ROUNDS));
    }

    /**
     * Verify a password against a hashed password
     *
     * @param plainPassword The plain text password to verify
     * @param hashedPassword The hashed password to check against
     * @return true if the password matches, false otherwise
     */
    public static boolean verifyPassword(String plainPassword, String hashedPassword) {
        try {
            return BCrypt.checkpw(plainPassword, hashedPassword);
        } catch (IllegalArgumentException e) {
            // Invalid hash format (e.g., legacy plain text password)
            return false;
        }
    }

    /**
     * Check if a string is a valid BCrypt hash
     *
     * @param password The string to check
     * @return true if it's a BCrypt hash, false otherwise
     */
    public static boolean isBCryptHash(String password) {
        // BCrypt hashes start with $2a$, $2b$, or $2y$ and are 60 characters long
        return password != null &&
               password.length() == 60 &&
               (password.startsWith("$2a$") ||
                password.startsWith("$2b$") ||
                password.startsWith("$2y$"));
    }
}

