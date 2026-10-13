package com.greenfield.scsp.domain;

/**
 * Priority levels set by council employees (FR-07). Each level carries a
 * target resolution time in hours, which makes the service level measurable.
 */
public enum Priority {
    LOW(240),
    MEDIUM(72),
    HIGH(24),
    URGENT(4);

    private final int targetHours;

    Priority(int targetHours) {
        this.targetHours = targetHours;
    }

    /** @return target number of hours to resolve a request at this priority */
    public int getTargetHours() { return targetHours; }
}
