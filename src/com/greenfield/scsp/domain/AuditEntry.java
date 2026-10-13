package com.greenfield.scsp.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable record of a security-relevant action (FR-19, NFR-09).
 * Captures who did what, to which object, and when.
 *
 * @param timestamp when the action happened
 * @param actorId   user id (or the attempted email for failed logins)
 * @param action    action code, e.g. "LOGIN_FAILED"
 * @param details   human-readable detail
 */
public record AuditEntry(LocalDateTime timestamp, String actorId, String action, String details) {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    @Override
    public String toString() {
        return String.format("%s  %-28s %-18s %s",
                timestamp.format(FORMAT), actorId, action, details);
    }
}
