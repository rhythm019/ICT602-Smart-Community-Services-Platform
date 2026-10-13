package com.greenfield.scsp.exception;

/**
 * Thrown when a signed-in user attempts an action their role does not grant
 * (FR-03, NFR-01).
 */
public class AuthorizationException extends RuntimeException {

    /** @param message explanation of the denied action */
    public AuthorizationException(String message) {
        super(message);
    }
}
