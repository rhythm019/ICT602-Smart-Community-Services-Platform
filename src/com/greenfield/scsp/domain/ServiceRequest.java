package com.greenfield.scsp.domain;

import com.greenfield.scsp.exception.BusinessRuleException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A community issue reported by a resident and worked on by council staff
 * (FR-04 to FR-09, FR-15).
 * <p>
 * The class guards its own state changes, so business rules are enforced no
 * matter which service or screen calls it:
 * <ul>
 *   <li>BR-02 - category, description and location are mandatory</li>
 *   <li>BR-03 - cannot complete without an assigned team</li>
 *   <li>BR-08 - feedback only after completion, once</li>
 * </ul>
 * Composition: the request owns its {@link StatusUpdate} history and
 * {@link Feedback}; they are created and destroyed with it.
 */
public class ServiceRequest {

    /** Minimum description length so reports are actionable (BR-02). */
    public static final int MIN_DESCRIPTION_LENGTH = 10;
    /** Maximum description length. */
    public static final int MAX_DESCRIPTION_LENGTH = 500;

    private final String requestId;
    private final Resident reporter;
    private final IssueCategory category;
    private final String description;
    private final Location location;
    private final LocalDateTime submittedAt;
    private final List<StatusUpdate> history = new ArrayList<>();

    private RequestStatus status;
    private Priority priority;
    private MaintenanceTeam assignedTeam;
    private Feedback feedback;

    /**
     * Creates a new request in the SUBMITTED state after validating BR-02.
     *
     * @param requestId   unique id, e.g. "SR-1001"
     * @param reporter    resident who reported the issue (BR-01)
     * @param category    issue category
     * @param description what is wrong, 10 to 500 characters
     * @param location    GIS-resolved location
     * @param submittedAt submission time
     * @throws BusinessRuleException when a mandatory field is missing or invalid
     */
    public ServiceRequest(String requestId, Resident reporter, IssueCategory category,
                          String description, Location location, LocalDateTime submittedAt) {
        if (reporter == null) {
            throw new BusinessRuleException("BR-01", "A registered resident account is required.");
        }
        if (category == null) {
            throw new BusinessRuleException("BR-02", "Issue category is required.");
        }
        if (description == null || description.trim().length() < MIN_DESCRIPTION_LENGTH
                || description.trim().length() > MAX_DESCRIPTION_LENGTH) {
            throw new BusinessRuleException("BR-02", "Description must be "
                    + MIN_DESCRIPTION_LENGTH + "-" + MAX_DESCRIPTION_LENGTH + " characters.");
        }
        if (location == null) {
            throw new BusinessRuleException("BR-02", "A valid location is required.");
        }
        this.requestId = Objects.requireNonNull(requestId);
        this.reporter = reporter;
        this.category = category;
        this.description = description.trim();
        this.location = location;
        this.submittedAt = submittedAt;
        this.status = RequestStatus.SUBMITTED;
        history.add(new StatusUpdate(status, "Issue reported", reporter.getUserId(), submittedAt));
    }

    /**
     * Sets the priority and moves the request into review (FR-07).
     *
     * @param newPriority priority chosen by the council employee
     * @param byUserId    id of the employee making the change
     * @param now         time of change
     */
    public void prioritise(Priority newPriority, String byUserId, LocalDateTime now) {
        if (newPriority == null) {
            throw new BusinessRuleException("FR-07", "Priority is required.");
        }
        this.priority = newPriority;
        if (status == RequestStatus.SUBMITTED) {
            changeStatus(RequestStatus.UNDER_REVIEW, "Priority set to " + newPriority, byUserId, now);
        } else {
            history.add(new StatusUpdate(status, "Priority changed to " + newPriority, byUserId, now));
        }
    }

    /**
     * Assigns the request to a maintenance team (FR-08). Requires a priority first.
     *
     * @param team     team that will do the work
     * @param byUserId id of the assigning employee
     * @param now      time of change
     */
    public void assignTo(MaintenanceTeam team, String byUserId, LocalDateTime now) {
        if (team == null) {
            throw new BusinessRuleException("FR-08", "A maintenance team is required.");
        }
        if (priority == null) {
            throw new BusinessRuleException("FR-08", "Set a priority before assigning work.");
        }
        changeStatus(RequestStatus.ASSIGNED, "Assigned to " + team.getName(), byUserId, now);
        this.assignedTeam = team;
    }

    /**
     * Records progress or completion reported by the field team (FR-09, BR-03).
     *
     * @param newStatus IN_PROGRESS or COMPLETED
     * @param note      progress note
     * @param byUserId  id of the maintenance officer
     * @param now       time of change
     */
    public void updateWorkStatus(RequestStatus newStatus, String note, String byUserId,
                                 LocalDateTime now) {
        if (newStatus == RequestStatus.COMPLETED && assignedTeam == null) {
            throw new BusinessRuleException("BR-03",
                    "A request cannot be completed before it is assigned to a team.");
        }
        changeStatus(newStatus, note, byUserId, now);
    }

    /**
     * Rejects a request that is a duplicate or outside council responsibility.
     *
     * @param reason   explanation sent to the resident
     * @param byUserId id of the employee
     * @param now      time of change
     */
    public void reject(String reason, String byUserId, LocalDateTime now) {
        changeStatus(RequestStatus.REJECTED, "Rejected: " + reason, byUserId, now);
    }

    /**
     * Stores the resident's feedback (FR-15, BR-08).
     *
     * @param newFeedback rating and comment
     */
    public void addFeedback(Feedback newFeedback) {
        if (status != RequestStatus.COMPLETED) {
            throw new BusinessRuleException("BR-08", "Feedback is only allowed after completion.");
        }
        if (feedback != null) {
            throw new BusinessRuleException("BR-08", "Feedback has already been given.");
        }
        if (newFeedback.rating() < Feedback.MIN_RATING || newFeedback.rating() > Feedback.MAX_RATING) {
            throw new BusinessRuleException("FR-15", "Rating must be between 1 and 5.");
        }
        this.feedback = newFeedback;
    }

    /**
     * Single place where status changes happen, enforcing the state machine.
     */
    private void changeStatus(RequestStatus target, String note, String byUserId, LocalDateTime now) {
        if (!status.canMoveTo(target)) {
            throw new BusinessRuleException("STATE",
                    "Cannot move request from " + status + " to " + target + ".");
        }
        status = target;
        history.add(new StatusUpdate(target, note, byUserId, now));
    }

    /**
     * Tells whether the request is past its target resolution time.
     *
     * @param now current time
     * @return true when open and older than the priority's target hours
     */
    public boolean isOverdue(LocalDateTime now) {
        if (priority == null || status == RequestStatus.COMPLETED || status == RequestStatus.REJECTED) {
            return false;
        }
        return submittedAt.plusHours(priority.getTargetHours()).isBefore(now);
    }

    public String getRequestId() { return requestId; }
    public Resident getReporter() { return reporter; }
    public IssueCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public Location getLocation() { return location; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public RequestStatus getStatus() { return status; }
    public Priority getPriority() { return priority; }
    public MaintenanceTeam getAssignedTeam() { return assignedTeam; }
    public Feedback getFeedback() { return feedback; }
    public List<StatusUpdate> getHistory() { return Collections.unmodifiableList(history); }

    @Override
    public String toString() {
        return String.format("%-8s %-28s %-12s %-7s %s",
                requestId, category.getLabel(), status,
                priority == null ? "-" : priority, location.address());
    }
}
