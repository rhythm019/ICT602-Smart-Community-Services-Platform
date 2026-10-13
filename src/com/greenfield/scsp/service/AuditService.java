package com.greenfield.scsp.service;

import com.greenfield.scsp.domain.AuditEntry;
import com.greenfield.scsp.domain.Permission;
import com.greenfield.scsp.exception.AuthorizationException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Append-only audit trail for security and administrative actions
 * (FR-19, NFR-09). Entries cannot be edited or deleted through this class.
 */
public class AuditService {

    private final List<AuditEntry> entries = new ArrayList<>();
    private final Clock clock;

    /** @param clock time source (injected so tests can fix the time) */
    public AuditService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Records an action.
     *
     * @param actorId who performed it
     * @param action  action code
     * @param details extra detail
     */
    public void record(String actorId, String action, String details) {
        entries.add(new AuditEntry(LocalDateTime.now(clock), actorId, action, details));
    }

    /**
     * Returns the audit log. Only administrators may read it.
     *
     * @param session caller's session
     * @return read-only list of entries, oldest first
     */
    public List<AuditEntry> viewLog(Session session) {
        if (!session.user().hasPermission(Permission.VIEW_AUDIT_LOG)) {
            throw new AuthorizationException("Only administrators can view the audit log.");
        }
        return Collections.unmodifiableList(entries);
    }

    /** @return number of entries (used by tests) */
    public int size() {
        return entries.size();
    }

    /** @return the most recent entry, or null when empty (used by tests) */
    public AuditEntry latest() {
        return entries.isEmpty() ? null : entries.get(entries.size() - 1);
    }
}
