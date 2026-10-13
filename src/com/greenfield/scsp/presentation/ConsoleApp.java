package com.greenfield.scsp.presentation;

import com.greenfield.scsp.domain.Resident;
import com.greenfield.scsp.domain.Role;
import com.greenfield.scsp.service.AuthenticationService;
import com.greenfield.scsp.service.Session;

import java.util.List;
import java.util.Map;

/**
 * Entry screen of the command-line client (wireframe W-01).
 * <p>
 * Handles sign-in and registration, then hands control to the dashboard
 * that matches the user's role. Role-to-menu routing uses a map, so adding a
 * new role means adding one entry rather than editing branching logic.
 */
public class ConsoleApp {

    private final ConsoleIO io;
    private final AuthenticationService auth;
    private final Map<Role, RoleMenu> menus;

    /**
     * @param io    console helper
     * @param auth  authentication service
     * @param menus dashboard to show for each role
     */
    public ConsoleApp(ConsoleIO io, AuthenticationService auth, Map<Role, RoleMenu> menus) {
        this.io = io;
        this.auth = auth;
        this.menus = Map.copyOf(menus);
    }

    /** Runs the application until the user chooses Exit. */
    public void run() {
        io.heading("Greenfield City Council - Smart Community Services Platform");
        io.println("  Demo accounts are listed in README.md (password for all: Greenfield1)");
        while (true) {
            io.heading("Welcome");
            int choice = io.menu(List.of("Sign in", "Register as a resident", "Exit"));
            switch (choice) {
                case 1 -> signIn();
                case 2 -> register();
                default -> {
                    io.println("  Goodbye.");
                    return;
                }
            }
        }
    }

    /** Sign-in form; on success opens the role dashboard. */
    private void signIn() {
        String email = io.ask("Email");
        String password = io.askPassword("Password");
        try {
            Session session = auth.login(email, password);
            io.success("Signed in as " + session.user().getFullName()
                    + " (" + session.user().getRole() + ")");
            menus.get(session.user().getRole()).show(session);
            auth.logout(session);
            io.success("Signed out.");
        } catch (RuntimeException ex) {
            io.error(ex.getMessage());
        }
    }

    /** Resident self-registration form (FR-01). */
    private void register() {
        io.heading("Create a Resident Account");
        try {
            Resident resident = auth.registerResident(
                    io.ask("Full name"),
                    io.ask("Email"),
                    io.askPassword("Password (8+ chars, letter and number)"),
                    io.ask("Mobile number (optional)"),
                    io.ask("Home address"));
            io.success("Account " + resident.getUserId() + " created. You can now sign in.");
        } catch (RuntimeException ex) {
            io.error(ex.getMessage());
        }
    }
}
