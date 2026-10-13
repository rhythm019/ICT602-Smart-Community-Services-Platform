package com.greenfield.scsp.service;

import com.greenfield.scsp.domain.Feedback;
import com.greenfield.scsp.domain.IssueCategory;
import com.greenfield.scsp.domain.Location;
import com.greenfield.scsp.domain.MaintenanceOfficer;
import com.greenfield.scsp.domain.MaintenanceTeam;
import com.greenfield.scsp.domain.Permission;
import com.greenfield.scsp.domain.Priority;
import com.greenfield.scsp.domain.RequestStatus;
import com.greenfield.scsp.domain.Resident;
import com.greenfield.scsp.domain.ServiceRequest;
import com.greenfield.scsp.domain.User;
import com.greenfield.scsp.exception.AuthorizationException;
import com.greenfield.scsp.exception.BusinessRuleException;
import com.greenfield.scsp.integration.GisService;
import com.greenfield.scsp.repository.MaintenanceTeamRepository;
import com.greenfield.scsp.repository.ServiceRequestRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Application service for the Community Issue Management module
 * (FR-04 to FR-09, FR-15, FR-20).
 * <p>
 * Coordinates the workflow but leaves the rules to the domain objects:
 * it checks permissions, calls GIS, saves through the repository, then
 * notifies and audits. This keeps each class focused on one job
 * (Single Responsibility Principle).
 */
public class ServiceRequestService {

    private final ServiceRequestRepository requests;
    private final MaintenanceTeamRepository teams;
    private final GisService gis;
    private final NotificationService notifier;
    private final AuditService audit;
    private final Clock clock;

    /**
     * All dependencies are injected through the constructor
     * (Dependency Inversion Principle).
     */
    public ServiceRequestService(ServiceRequestRepository requests, MaintenanceTeamRepository teams,
                                 GisService gis, NotificationService notifier,
                                 AuditService audit, Clock clock) {
        this.requests = requests;
        this.teams = teams;
        this.gis = gis;
        this.notifier = notifier;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Reports a new community issue (FR-04, FR-05, BR-01, BR-02).
     *
     * @param session     resident's session
     * @param category    issue category
     * @param description issue description
     * @param address     street address to geocode
     * @return the saved request with its new id
     */
    public ServiceRequest reportIssue(Session session, IssueCategory category,
                                      String description, String address) {
        requirePermission(session, Permission.REPORT_ISSUE);
        Location location = gis.geocode(address).orElseThrow(() -> new BusinessRuleException(
                "BR-02", "Location not found inside the Greenfield council area."));

        Resident reporter = (Resident) session.user();
        ServiceRequest request = new ServiceRequest(requests.nextId(), reporter, category,
                description, location, now());
        requests.save(request);

        audit.record(reporter.getUserId(), "ISSUE_REPORTED", request.getRequestId());
        notifier.notify(reporter, "Request " + request.getRequestId() + " received",
                "Thanks for reporting " + category.getLabel().toLowerCase()
                        + " at " + location.address() + ". We will keep you updated.");
        return request;
    }

    /**
     * Lists the signed-in resident's requests, newest first (FR-06).
     *
     * @param session resident's session
     * @return the resident's requests
     */
    public List<ServiceRequest> myRequests(Session session) {
        requirePermission(session, Permission.VIEW_OWN_REQUESTS);
        return requests.findByReporter(session.user().getUserId()).stream()
                .sorted(Comparator.comparing(ServiceRequest::getSubmittedAt).reversed())
                .toList();
    }

    /**
     * Finds one request the caller is allowed to see (FR-06).
     * Residents can only see their own requests; staff can see any.
     *
     * @param session   caller's session
     * @param requestId request id
     * @return the request
     */
    public ServiceRequest viewRequest(Session session, String requestId) {
        ServiceRequest request = find(requestId);
        User user = session.user();
        boolean isOwner = request.getReporter().getUserId().equals(user.getUserId());
        boolean isStaff = user.hasPermission(Permission.VIEW_ALL_REQUESTS);
        boolean isAssignedOfficer = user instanceof MaintenanceOfficer officer
                && officer.getTeam() != null
                && request.getAssignedTeam() == officer.getTeam();
        if (!isOwner && !isStaff && !isAssignedOfficer) {
            throw new AuthorizationException("You can only view your own requests.");
        }
        return request;
    }

    /**
     * Returns open requests ordered by priority then age, for the council
     * queue (wireframe W-04, FR-20).
     *
     * @param session staff session
     * @param filter  optional status filter, or null for all open requests
     * @return ordered queue
     */
    public List<ServiceRequest> requestQueue(Session session, RequestStatus filter) {
        requirePermission(session, Permission.VIEW_ALL_REQUESTS);
        Comparator<ServiceRequest> byPriority = Comparator.comparing(
                (ServiceRequest r) -> r.getPriority() == null ? -1 : r.getPriority().ordinal())
                .reversed()
                .thenComparing(ServiceRequest::getSubmittedAt);
        return requests.findAll().stream()
                .filter(r -> filter == null
                        ? r.getStatus() != RequestStatus.COMPLETED && r.getStatus() != RequestStatus.REJECTED
                        : r.getStatus() == filter)
                .sorted(byPriority)
                .toList();
    }

    /**
     * Sets a request's priority (FR-07, BR-06).
     *
     * @param session   council employee's session
     * @param requestId request id
     * @param priority  new priority
     * @return updated request
     */
    public ServiceRequest prioritise(Session session, String requestId, Priority priority) {
        requirePermission(session, Permission.PRIORITISE_REQUEST);
        ServiceRequest request = find(requestId);
        request.prioritise(priority, session.user().getUserId(), now());
        requests.save(request);
        audit.record(session.user().getUserId(), "PRIORITY_SET", requestId + " -> " + priority);
        return request;
    }

    /**
     * Assigns a request to a maintenance team and notifies everyone involved (FR-08).
     *
     * @param session   council employee's session
     * @param requestId request id
     * @param teamId    team id
     * @return updated request
     */
    public ServiceRequest assign(Session session, String requestId, String teamId) {
        requirePermission(session, Permission.ASSIGN_WORK);
        ServiceRequest request = find(requestId);
        MaintenanceTeam team = teams.findById(teamId).orElseThrow(
                () -> new BusinessRuleException("FR-08", "Maintenance team " + teamId + " not found."));
        request.assignTo(team, session.user().getUserId(), now());
        requests.save(request);

        audit.record(session.user().getUserId(), "WORK_ASSIGNED", requestId + " -> " + team.getTeamId());
        notifier.notify(request.getReporter(), "Request " + requestId + " update",
                "Your request has been assigned to " + team.getName() + ".");
        for (MaintenanceOfficer officer : team.getMembers()) {
            notifier.notify(officer, "New work: " + requestId,
                    request.getCategory().getLabel() + " at " + request.getLocation().address()
                            + " (priority " + request.getPriority() + ")");
        }
        return request;
    }

    /**
     * Rejects a request with a reason (e.g. duplicate).
     *
     * @param session   council employee's session
     * @param requestId request id
     * @param reason    reason sent to the resident
     * @return updated request
     */
    public ServiceRequest reject(Session session, String requestId, String reason) {
        requirePermission(session, Permission.PRIORITISE_REQUEST);
        ServiceRequest request = find(requestId);
        request.reject(reason, session.user().getUserId(), now());
        requests.save(request);
        audit.record(session.user().getUserId(), "REQUEST_REJECTED", requestId);
        notifier.notify(request.getReporter(), "Request " + requestId + " closed", reason);
        return request;
    }

    /**
     * Lists the work assigned to the officer's team (FR-09).
     *
     * @param session maintenance officer's session
     * @return team's active work
     */
    public List<ServiceRequest> myTeamWork(Session session) {
        requirePermission(session, Permission.UPDATE_WORK_STATUS);
        MaintenanceTeam team = ((MaintenanceOfficer) session.user()).getTeam();
        if (team == null) {
            return List.of();
        }
        return requests.findByTeam(team.getTeamId()).stream()
                .filter(r -> r.getStatus() == RequestStatus.ASSIGNED
                        || r.getStatus() == RequestStatus.IN_PROGRESS)
                .toList();
    }

    /**
     * Updates work progress (FR-09, BR-03). Officers may only update work
     * assigned to their own team.
     *
     * @param session   maintenance officer's session
     * @param requestId request id
     * @param newStatus IN_PROGRESS or COMPLETED
     * @param note      progress note
     * @return updated request
     */
    public ServiceRequest updateWork(Session session, String requestId,
                                     RequestStatus newStatus, String note) {
        requirePermission(session, Permission.UPDATE_WORK_STATUS);
        ServiceRequest request = find(requestId);
        MaintenanceOfficer officer = (MaintenanceOfficer) session.user();
        if (request.getAssignedTeam() == null || request.getAssignedTeam() != officer.getTeam()) {
            throw new AuthorizationException("This request is not assigned to your team.");
        }
        request.updateWorkStatus(newStatus, note, officer.getUserId(), now());
        requests.save(request);

        audit.record(officer.getUserId(), "WORK_UPDATED", requestId + " -> " + newStatus);
        String message = newStatus == RequestStatus.COMPLETED
                ? "Your request is complete. Please rate the service in the app."
                : "Work has started: " + note;
        notifier.notify(request.getReporter(), "Request " + requestId + " update", message);
        return request;
    }

    /**
     * Records the resident's feedback on their own completed request (FR-15, BR-08).
     *
     * @param session   resident's session
     * @param requestId request id
     * @param rating    1 to 5
     * @param comment   optional comment
     * @return updated request
     */
    public ServiceRequest giveFeedback(Session session, String requestId, int rating, String comment) {
        requirePermission(session, Permission.GIVE_FEEDBACK);
        ServiceRequest request = find(requestId);
        if (!request.getReporter().getUserId().equals(session.user().getUserId())) {
            throw new AuthorizationException("You can only rate your own requests.");
        }
        request.addFeedback(new Feedback(rating, comment, now()));
        requests.save(request);
        audit.record(session.user().getUserId(), "FEEDBACK_GIVEN", requestId + " rating " + rating);
        return request;
    }

    /** @return all maintenance teams (for assignment menus) */
    public List<MaintenanceTeam> listTeams() {
        return teams.findAll();
    }

    /** Throws when the caller's role lacks a permission (NFR-01). */
    private void requirePermission(Session session, Permission permission) {
        if (session == null) {
            throw new AuthorizationException("Please sign in first.");
        }
        if (!session.user().hasPermission(permission)) {
            audit.record(session.user().getUserId(), "ACCESS_DENIED", permission.name());
            throw new AuthorizationException("Your role does not allow: " + permission);
        }
    }

    /** Loads a request or fails with a clear message. */
    private ServiceRequest find(String requestId) {
        return requests.findById(requestId).orElseThrow(
                () -> new BusinessRuleException("FR-06", "Request " + requestId + " not found."));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
