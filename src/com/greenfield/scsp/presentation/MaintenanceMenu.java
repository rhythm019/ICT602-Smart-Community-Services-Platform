package com.greenfield.scsp.presentation;

import com.greenfield.scsp.domain.RequestStatus;
import com.greenfield.scsp.domain.ServiceRequest;
import com.greenfield.scsp.service.ServiceRequestService;
import com.greenfield.scsp.service.Session;

import java.util.List;

/**
 * Maintenance officer dashboard: view the team's work and update progress
 * (FR-09).
 */
public class MaintenanceMenu implements RoleMenu {

    private final ConsoleIO io;
    private final ServiceRequestService requestService;

    /**
     * @param io             console helper
     * @param requestService request workflow service
     */
    public MaintenanceMenu(ConsoleIO io, ServiceRequestService requestService) {
        this.io = io;
        this.requestService = requestService;
    }

    @Override
    public void show(Session session) {
        while (true) {
            io.heading("Maintenance Dashboard - " + session.user().getFullName());
            int choice = io.menu(List.of("My team's work", "Mark work as started",
                    "Mark work as completed", "Sign out"));
            try {
                switch (choice) {
                    case 1 -> listWork(session);
                    case 2 -> update(session, RequestStatus.IN_PROGRESS);
                    case 3 -> update(session, RequestStatus.COMPLETED);
                    default -> { return; }
                }
            } catch (RuntimeException ex) {
                io.error(ex.getMessage());
            }
        }
    }

    private void listWork(Session session) {
        List<ServiceRequest> work = requestService.myTeamWork(session);
        if (work.isEmpty()) {
            io.println("  No open work for your team.");
            return;
        }
        work.forEach(r -> io.println("  " + r));
    }

    private void update(Session session, RequestStatus status) {
        String id = io.ask("Request ID");
        String note = io.ask("Progress note");
        requestService.updateWork(session, id, status, note.isBlank() ? status.name() : note);
        io.success(id.toUpperCase() + " is now " + status + ". Resident notified.");
    }
}
