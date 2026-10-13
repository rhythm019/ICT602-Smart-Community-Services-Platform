package com.greenfield.scsp.domain;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One entry in a request's history (FR-06, FR-09). Owned by its
 * {@link ServiceRequest} through composition: it has no meaning on its own.
 *
 * @param status    status after the change
 * @param note      short comment shown to the resident
 * @param updatedBy user id of whoever made the change
 * @param timestamp when the change happened
 */
public record StatusUpdate(RequestStatus status, String note, String updatedBy,
                           LocalDateTime timestamp) {

    private static final DateTimeFormatter FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public String toString() {
        return String.format("%s  %-12s %s (by %s)",
                timestamp.format(FORMAT), status, note, updatedBy);
    }
}
