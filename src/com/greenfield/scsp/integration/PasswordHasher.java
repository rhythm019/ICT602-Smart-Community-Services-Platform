package com.greenfield.scsp.integration;

/**
 * Hashes and verifies passwords. Passwords are never stored in plain text
 * (NFR-02).
 */
public interface PasswordHasher {

    /**
     * Produces a salted hash of a password.
     *
     * @param rawPassword password typed by the user
     * @return encoded string containing salt and hash
     */
    String hash(String rawPassword);

    /**
     * Checks a password against a stored hash.
     *
     * @param rawPassword password typed by the user
     * @param storedHash  value produced earlier by {@link #hash(String)}
     * @return true when they match
     */
    boolean matches(String rawPassword, String storedHash);
}
