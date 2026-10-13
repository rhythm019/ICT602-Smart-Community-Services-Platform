package com.greenfield.scsp.presentation;

import com.greenfield.scsp.domain.MaintenanceTeam;
import com.greenfield.scsp.domain.Priority;
import com.greenfield.scsp.domain.RequestStatus;
import com.greenfield.scsp.domain.ServiceRequest;
import com.greenfield.scsp.service.ServiceRequestService;
import com.greenfield.scsp.service.Session;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Council employee dashboard (wireframe W-04): priority queue, prioritise,
 * assign to a team and reject.
 */
public class CouncilEmployeeMenu implements RoleMenu {

    private final ConsoleIO io;
    private final ServiceRequestService requestService;
    private final Clock clock;

    /**
     * @param io             console helper
     * @param requestService request workflow service
     * @param clock          time source used to flag overdue work
     */
    public CouncilEmployeeMenu(ConsoleIO io, ServiceRequestService requestService, Clock clock) {
        this.io = io;
        this.requestService = requestService;
        this.clock = clock;
    }

    @Override
    public void show(Session session) {
        while (true) {
            io.heading("Council Employee Dashboard - " + session.user().getFullName());
            int choice = io.menu(List.of("View open request queue", "Filter queue by status",
                    "Set priority", "Assign to maintenance team", "Reject request",
                    "View request details", "Sign out"));
            try {
                switch (choice) {
                    case 1 -> printQueue(requestService.requestQueue(session, null));
                    case 2 -> filterQueue(session);
                    case 3 -> setPriority(session);
                    case 4 -> assign(session);
                    case 5 -> reject(session);
                    case 6 -> details(session);
                    default -> { return; }
                }
            } catch (RuntimeException ex) {
                io.error(ex.getMessage());
            }
        }
    }

    /** Prints the priority queue, flagging overdue items. */
    private void printQueue(List<ServiceRequest> queue) {
        if (queue.isEmpty()) {
            io.println("  No matching requests.");
            return;
        }
        io.println(String.format("  %-8s %-28s %-12s %-7s %s", "ID", "Category", "Status", "Prio", "Address"));
        LocalDateTime now = LocalDateTime.now(clock);
        for (ServiceRequest r : queue) {
            io.println("  " + r + (r.isOverdue(now) ? "   << OVERDUE" : ""));
        }
    }

    private void filterQueue(Session session) {
        RequestStatus[] statuses = RequestStatus.values();
        int pick = io.menu(Arrays.stream(statuses).map(Enum::name).toList());
        if (pick > 0) {
            printQueue(requestService.requestQueue(session, statuses[pick - 1]));
        }
    }

    private void setPriority(Session session) {
        String id = io.ask("Request ID");
        Priority[] levels = Priority.values();
        int pick = io.menu(Arrays.stream(levels)
                .map(p -> p + " (target " + p.getTargetHours() + "h)").toList());
        if (pick > 0) {
            requestService.prioritise(session, id, levels[pick - 1]);
            io.success(id.toUpperCase() + " priority set to " + levels[pick - 1] + ".");
        }
    }

    private void assign(Session session) {
        String id = io.ask("Request ID");
        List<MaintenanceTeam> teams = requestService.listTeams();
        int pick = io.menu(teams.stream().map(MaintenanceTeam::toString).toList());
        if (pick > 0) {
            MaintenanceTeam team = teams.get(pick - 1);
            requestService.assign(session, id, team.getTeamId());
            io.success(id.toUpperCase() + " assigned to " + team.getName() + ". Team notified.");
        }
    }

    private void reject(Session session) {
        String id = io.ask("Request ID");
        String reason = io.ask("Reason (sent to resident)");
        requestService.reject(session, id, reason);
        io.success(id.toUpperCase() + " rejected.");
    }

    private void details(Session session) {
        ServiceRequest r = requestService.viewRequest(session, io.ask("Request ID"));
        io.heading("Request " + r.getRequestId() + " - reported by " + r.getReporter().getFullName());
        io.println("  " + r.getDescription());
        r.getHistory().forEach(u -> io.println("    " + u));
    }
}
