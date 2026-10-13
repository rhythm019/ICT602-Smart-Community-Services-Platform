package com.greenfield.scsp.presentation;

import com.greenfield.scsp.domain.IssueCategory;
import com.greenfield.scsp.domain.Notification;
import com.greenfield.scsp.domain.ServiceRequest;
import com.greenfield.scsp.domain.StatusUpdate;
import com.greenfield.scsp.integration.InAppChannel;
import com.greenfield.scsp.service.ServiceRequestService;
import com.greenfield.scsp.service.Session;

import java.util.Arrays;
import java.util.List;

/**
 * Resident dashboard (wireframes W-02 and W-03): report an issue, track
 * requests, read notifications and give feedback.
 */
public class ResidentMenu implements RoleMenu {

    private final ConsoleIO io;
    private final ServiceRequestService requestService;
    private final InAppChannel inbox;

    /**
     * @param io             console helper
     * @param requestService request workflow service
     * @param inbox          in-app notification inbox
     */
    public ResidentMenu(ConsoleIO io, ServiceRequestService requestService, InAppChannel inbox) {
        this.io = io;
        this.requestService = requestService;
        this.inbox = inbox;
    }

    @Override
    public void show(Session session) {
        while (true) {
            io.heading("Resident Dashboard - " + session.user().getFullName());
            int choice = io.menu(List.of("Report an issue", "My requests", "View request details",
                    "Give feedback on a completed request", "Notifications", "Sign out"));
            try {
                switch (choice) {
                    case 1 -> reportIssue(session);
                    case 2 -> listMyRequests(session);
                    case 3 -> viewDetails(session);
                    case 4 -> giveFeedback(session);
                    case 5 -> showNotifications(session);
                    default -> { return; }
                }
            } catch (RuntimeException ex) {
                // Business and permission errors are shown, then the menu repeats.
                io.error(ex.getMessage());
            }
        }
    }

    /** Report-an-issue form (W-03). */
    private void reportIssue(Session session) {
        io.heading("Report an Issue");
        IssueCategory[] categories = IssueCategory.values();
        List<String> labels = Arrays.stream(categories).map(IssueCategory::getLabel).toList();
        int pick = io.menu(labels);
        if (pick == 0) {
            return;
        }
        String description = io.ask("Describe the issue (10-500 characters)");
        String address = io.ask("Street address (must be in Greenfield)");
        ServiceRequest request = requestService.reportIssue(
                session, categories[pick - 1], description, address);
        io.success("Request " + request.getRequestId() + " submitted. Location: " + request.getLocation());
    }

    /** My Requests table (W-02). */
    private void listMyRequests(Session session) {
        io.heading("My Requests");
        List<ServiceRequest> mine = requestService.myRequests(session);
        if (mine.isEmpty()) {
            io.println("  You have not reported any issues yet.");
            return;
        }
        io.println(String.format("  %-8s %-28s %-12s %-7s %s", "ID", "Category", "Status", "Prio", "Address"));
        mine.forEach(r -> io.println("  " + r));
    }

    /** Shows the full history of one request. */
    private void viewDetails(Session session) {
        String id = io.ask("Request ID (e.g. SR-1001)");
        ServiceRequest r = requestService.viewRequest(session, id);
        io.heading("Request " + r.getRequestId());
        io.println("  Category   : " + r.getCategory().getLabel());
        io.println("  Description: " + r.getDescription());
        io.println("  Location   : " + r.getLocation());
        io.println("  Status     : " + r.getStatus());
        io.println("  Team       : " + (r.getAssignedTeam() == null ? "-" : r.getAssignedTeam().getName()));
        io.println("  History:");
        for (StatusUpdate u : r.getHistory()) {
            io.println("    " + u);
        }
    }

    /** Feedback form (FR-15). */
    private void giveFeedback(Session session) {
        String id = io.ask("Request ID");
        int rating = io.askInt("Rating", 1, 5);
        String comment = io.ask("Comment (optional)");
        requestService.giveFeedback(session, id, rating, comment);
        io.success("Thank you for your feedback.");
    }

    /** Lists in-app notifications, newest first. */
    private void showNotifications(Session session) {
        io.heading("Notifications");
        List<Notification> mine = inbox.inboxFor(session.user().getUserId());
        if (mine.isEmpty()) {
            io.println("  No notifications.");
        }
        mine.forEach(n -> io.println("  - " + n.subject() + ": " + n.message()));
    }
}
