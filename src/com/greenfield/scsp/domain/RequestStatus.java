package com.greenfield.scsp.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Life-cycle states of a {@link ServiceRequest}.
 * <p>
 * The allowed transitions form a small state machine. Keeping the rules here
 * means no other class can move a request into an invalid state
 * (e.g. BR-03: a request cannot be completed before it is assigned).
 */
public enum RequestStatus {
    SUBMITTED,
    UNDER_REVIEW,
    ASSIGNED,
    IN_PROGRESS,
    COMPLETED,
    REJECTED;

    /**
     * Returns the states this status may move to next.
     *
     * @return set of permitted next states
     */
    public Set<RequestStatus> nextStates() {
        return switch (this) {
            case SUBMITTED -> EnumSet.of(UNDER_REVIEW, REJECTED);
            case UNDER_REVIEW -> EnumSet.of(ASSIGNED, REJECTED);
            case ASSIGNED -> EnumSet.of(IN_PROGRESS, COMPLETED);
            case IN_PROGRESS -> EnumSet.of(COMPLETED);
            case COMPLETED, REJECTED -> EnumSet.noneOf(RequestStatus.class);
        };
    }

    /**
     * Checks whether moving to the target state is allowed.
     *
     * @param target proposed next state
     * @return true when the transition is valid
     */
    public boolean canMoveTo(RequestStatus target) {
        return nextStates().contains(target);
    }
}
