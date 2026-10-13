package com.greenfield.scsp.service;

import com.greenfield.scsp.domain.Administrator;
import com.greenfield.scsp.domain.CouncilEmployee;
import com.greenfield.scsp.domain.MaintenanceOfficer;
import com.greenfield.scsp.domain.MaintenanceTeam;
import com.greenfield.scsp.domain.Permission;
import com.greenfield.scsp.domain.Role;
import com.greenfield.scsp.domain.User;
import com.greenfield.scsp.exception.AuthorizationException;
import com.greenfield.scsp.exception.BusinessRuleException;
import com.greenfield.scsp.integration.PasswordHasher;
import com.greenfield.scsp.repository.MaintenanceTeamRepository;
import com.greenfield.scsp.repository.UserRepository;

import java.util.List;

/**
 * Administrator functions for staff accounts (FR-18, BR-07).
 * <p>
 * Uses a small factory method ({@link #createStaff}) to build the right
 * {@link User} subclass for a requested {@link Role}.
 */
public class UserAdministrationService {

    private final UserRepository users;
    private final MaintenanceTeamRepository teams;
    private final PasswordHasher hasher;
    private final AuthenticationService auth;
    private final AuditService audit;

    /** All dependencies are injected by the composition root. */
    public UserAdministrationService(UserRepository users, MaintenanceTeamRepository teams,
                                     PasswordHasher hasher, AuthenticationService auth,
                                     AuditService audit) {
        this.users = users;
        this.teams = teams;
        this.hasher = hasher;
        this.auth = auth;
        this.audit = audit;
    }

    /**
     * Creates a staff account (factory method).
     *
     * @param session    administrator's session
     * @param role       role of the new account (not RESIDENT; residents self-register)
     * @param fullName   staff member's name
     * @param email      work email
     * @param password   initial password
     * @param extra      department for employees, team id for maintenance officers
     * @return the created user
     */
    public User createStaff(Session session, Role role, String fullName, String email,
                            String password, String extra) {
        requireAdmin(session);
        if (role == Role.RESIDENT) {
            throw new BusinessRuleException("FR-01", "Residents create their own accounts.");
        }
        if (users.findByEmail(email).isPresent()) {
            throw new BusinessRuleException("FR-18", "An account with this email already exists.");
        }
        auth.validatePassword(password);
        String id = users.nextId();
        String hash = hasher.hash(password);

        User created = switch (role) {
            case COUNCIL_EMPLOYEE -> new CouncilEmployee(id, fullName, email, hash, extra);
            case ADMINISTRATOR -> new Administrator(id, fullName, email, hash);
            case MAINTENANCE_OFFICER -> {
                MaintenanceOfficer officer = new MaintenanceOfficer(id, fullName, email, hash);
                MaintenanceTeam team = teams.findById(extra).orElseThrow(
                        () -> new BusinessRuleException("FR-18", "Team " + extra + " not found."));
                team.addMember(officer);
                yield officer;
            }
            case RESIDENT -> throw new IllegalStateException("Handled above");
        };
        users.save(created);
        audit.record(session.user().getUserId(), "USER_CREATED", id + " as " + role);
        return created;
    }

    /**
     * Enables or disables an account. Administrators cannot disable themselves,
     * which prevents locking everyone out of the system.
     *
     * @param session administrator's session
     * @param userId  account to change
     * @param active  true to enable, false to disable
     * @return the changed user
     */
    public User setActive(Session session, String userId, boolean active) {
        requireAdmin(session);
        if (session.user().getUserId().equals(userId) && !active) {
            throw new BusinessRuleException("BR-07", "You cannot disable your own account.");
        }
        User user = users.findById(userId).orElseThrow(
                () -> new BusinessRuleException("FR-18", "User " + userId + " not found."));
        user.setActive(active);
        users.save(user);
        audit.record(session.user().getUserId(), active ? "USER_ENABLED" : "USER_DISABLED", userId);
        return user;
    }

    /**
     * Lists all accounts.
     *
     * @param session administrator's session
     * @return all users
     */
    public List<User> listUsers(Session session) {
        requireAdmin(session);
        return users.findAll();
    }

    /** Only administrators may manage users (BR-07). */
    private void requireAdmin(Session session) {
        if (session == null || !session.user().hasPermission(Permission.MANAGE_USERS)) {
            if (session != null) {
                audit.record(session.user().getUserId(), "ACCESS_DENIED", "MANAGE_USERS");
            }
            throw new AuthorizationException("Only administrators can manage user accounts.");
        }
    }
}
