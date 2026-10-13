package com.greenfield.scsp.presentation;

import com.greenfield.scsp.domain.Role;
import com.greenfield.scsp.domain.User;
import com.greenfield.scsp.service.AuditService;
import com.greenfield.scsp.service.Session;
import com.greenfield.scsp.service.UserAdministrationService;

import java.util.List;

/**
 * Administrator dashboard: manage staff accounts and read the audit log
 * (FR-18, FR-19).
 */
public class AdminMenu implements RoleMenu {

    private final ConsoleIO io;
    private final UserAdministrationService adminService;
    private final AuditService auditService;

    /**
     * @param io           console helper
     * @param adminService user administration service
     * @param auditService audit trail
     */
    public AdminMenu(ConsoleIO io, UserAdministrationService adminService, AuditService auditService) {
        this.io = io;
        this.adminService = adminService;
        this.auditService = auditService;
    }

    @Override
    public void show(Session session) {
        while (true) {
            io.heading("Administrator Dashboard - " + session.user().getFullName());
            int choice = io.menu(List.of("List users", "Create staff account",
                    "Disable an account", "Enable an account", "View audit log", "Sign out"));
            try {
                switch (choice) {
                    case 1 -> adminService.listUsers(session).forEach(u -> io.println("  " + u));
                    case 2 -> createStaff(session);
                    case 3 -> toggle(session, false);
                    case 4 -> toggle(session, true);
                    case 5 -> auditService.viewLog(session).forEach(e -> io.println("  " + e));
                    default -> { return; }
                }
            } catch (RuntimeException ex) {
                io.error(ex.getMessage());
            }
        }
    }

    private void createStaff(Session session) {
        List<Role> staffRoles = List.of(Role.COUNCIL_EMPLOYEE, Role.MAINTENANCE_OFFICER, Role.ADMINISTRATOR);
        int pick = io.menu(staffRoles.stream().map(Enum::name).toList());
        if (pick == 0) {
            return;
        }
        Role role = staffRoles.get(pick - 1);
        String name = io.ask("Full name");
        String email = io.ask("Work email");
        String password = io.askPassword("Initial password");
        String extra = switch (role) {
            case COUNCIL_EMPLOYEE -> io.ask("Department");
            case MAINTENANCE_OFFICER -> io.ask("Team ID (T-ROADS, T-WASTE, T-PARKS)");
            default -> "";
        };
        User created = adminService.createStaff(session, role, name, email, password, extra);
        io.success("Created " + created.getUserId() + " (" + role + ").");
    }

    private void toggle(Session session, boolean active) {
        String id = io.ask("User ID (e.g. U-0005)");
        User user = adminService.setActive(session, id.toUpperCase(), active);
        io.success(user.getUserId() + " is now " + (active ? "enabled" : "disabled") + ".");
    }
}
