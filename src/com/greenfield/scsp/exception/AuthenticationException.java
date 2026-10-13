package com.greenfield.scsp.exception;

/**
 * Thrown when sign-in fails. The message is deliberately generic so it does
 * not reveal whether the email exists (OWASP ASVS V6 guidance).
 */
public class AuthenticationException extends RuntimeException {

    /** @param message message safe to show to the user */
    public AuthenticationException(String message) {
        super(message);
    }
}
