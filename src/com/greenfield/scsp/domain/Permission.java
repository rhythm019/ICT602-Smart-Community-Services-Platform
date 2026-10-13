package com.greenfield.scsp.domain;

/**
 * Fine-grained actions that can be granted to a {@link Role}.
 * <p>
 * Permissions are checked by the service layer before any protected
 * operation runs (SRS NFR-01, BR-06, BR-07).
 */
public enum Permission {
    /** Submit a new community issue (FR-04). */
    REPORT_ISSUE,
    /** View and track the user's own requests (FR-06). */
    VIEW_OWN_REQUESTS,
    /** Give feedback on a completed request (FR-15). */
    GIVE_FEEDBACK,
    /** View all requests in the council queue (FR-20). */
    VIEW_ALL_REQUESTS,
    /** Set or change the priority of a request (FR-07, BR-06). */
    PRIORITISE_REQUEST,
    /** Assign a request to a maintenance team (FR-08). */
    ASSIGN_WORK,
    /** Update progress on assigned work (FR-09). */
    UPDATE_WORK_STATUS,
    /** Create, disable and change roles of user accounts (FR-18, BR-07). */
    MANAGE_USERS,
    /** Read the security audit log (FR-19). */
    VIEW_AUDIT_LOG
}
