package com.ia.ia_base.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * IB Security — password hashing
 *
 * Passwords are NEVER stored as plain text.
 * BCrypt turns a password into a one-way hash.
 * When a user logs in, we hash their input and compare hashes — not passwords.
 *
 * BCrypt also adds a random "salt" so two users with the same password
 * get different hashes (makes database leaks harder to exploit).
 */
public class PasswordHasher {
    
    /** Returns the BCrypt hash of a plain-text password. */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty");
        }
        // BCrypt automatically generates a salt and includes it in the hash
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }
    
    /** Returns true if the plain-text password matches the stored hash. */
    public static boolean verifyPassword(String password, String hash) {
        if (password == null || hash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, hash);
        } catch (Exception e) {
            return false;
        }
    }
}

