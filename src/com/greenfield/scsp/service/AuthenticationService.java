package com.greenfield.scsp.service;

import com.greenfield.scsp.domain.Resident;
import com.greenfield.scsp.domain.User;
import com.greenfield.scsp.exception.AuthenticationException;
import com.greenfield.scsp.exception.BusinessRuleException;
import com.greenfield.scsp.integration.PasswordHasher;
import com.greenfield.scsp.repository.UserRepository;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Handles resident self-registration and sign-in (FR-01, FR-02).
 * <p>
 * Security controls implemented here:
 * <ul>
 *   <li>passwords stored only as PBKDF2 salted hashes</li>
 *   <li>minimum 8-character password with a letter and a digit</li>
 *   <li>account locks for 15 minutes after 5 failed attempts</li>
 *   <li>generic error messages that do not reveal whether an email exists</li>
 *   <li>every success and failure is written to the audit log</li>
 * </ul>
 */
public class AuthenticationService {

    /** Minimum password length (NIST SP 800-63B). */
    public static final int MIN_PASSWORD_LENGTH = 8;

    private static final Pattern EMAIL =
            Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final String GENERIC_FAILURE = "Invalid email or password.";

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final AuditService audit;
    private final Clock clock;

    /**
     * @param users  user store
     * @param hasher password hasher
     * @param audit  audit trail
     * @param clock  time source
     */
    public AuthenticationService(UserRepository users, PasswordHasher hasher,
                                 AuditService audit, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.audit = audit;
        this.clock = clock;
    }

    /**
     * Registers a new resident (FR-01).
     *
     * @param fullName resident name
     * @param email    login email, must be unique
     * @param password password meeting the policy
     * @param phone    mobile number for SMS (optional)
     * @param address  home address
     * @return the created resident
     * @throws BusinessRuleException when input is invalid or the email is taken
     */
    public Resident registerResident(String fullName, String email, String password,
                                     String phone, String address) {
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessRuleException("FR-01", "Full name is required.");
        }
        if (email == null || !EMAIL.matcher(email.trim()).matches()) {
            throw new BusinessRuleException("FR-01", "A valid email address is required.");
        }
        validatePassword(password);
        if (users.findByEmail(email).isPresent()) {
            throw new BusinessRuleException("FR-01", "An account with this email already exists.");
        }
        Resident resident = new Resident(users.nextId(), fullName.trim(), email.trim(),
                hasher.hash(password), phone, address);
        users.save(resident);
        audit.record(resident.getUserId(), "REGISTER", "Resident account created");
        return resident;
    }

    /**
     * Signs a user in (FR-02).
     *
     * @param email    login email
     * @param password password
     * @return a session for the authenticated user
     * @throws AuthenticationException when credentials are wrong or the account
     *                                 is locked or disabled
     */
    public Session login(String email, String password) {
        LocalDateTime now = LocalDateTime.now(clock);
        Optional<User> found = users.findByEmail(email);

        if (found.isEmpty()) {
            audit.record(String.valueOf(email), "LOGIN_FAILED", "Unknown account");
            throw new AuthenticationException(GENERIC_FAILURE);
        }
        User user = found.get();

        if (!user.isActive()) {
            audit.record(user.getUserId(), "LOGIN_BLOCKED", "Account disabled");
            throw new AuthenticationException("This account has been disabled. Contact the council.");
        }
        if (user.isLocked(now)) {
            audit.record(user.getUserId(), "LOGIN_BLOCKED", "Account locked");
            throw new AuthenticationException(
                    "Too many failed attempts. Try again in " + User.LOCKOUT_PERIOD.toMinutes() + " minutes.");
        }
        if (!hasher.matches(password, user.getPasswordHash())) {
            user.registerFailedLogin(now);
            audit.record(user.getUserId(), "LOGIN_FAILED",
                    "Attempt " + user.getFailedLoginAttempts() + " of " + User.MAX_FAILED_ATTEMPTS);
            throw new AuthenticationException(GENERIC_FAILURE);
        }

        user.resetFailedLogins();
        audit.record(user.getUserId(), "LOGIN_SUCCESS", user.getRole().name());
        return new Session(user, now);
    }

    /**
     * Ends a session and records it.
     *
     * @param session session being closed
     */
    public void logout(Session session) {
        audit.record(session.user().getUserId(), "LOGOUT", "User signed out");
    }

    /**
     * Applies the password policy.
     *
     * @param password candidate password
     * @throws BusinessRuleException when the policy is not met
     */
    public void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new BusinessRuleException("NFR-01", "Password must be at least "
                    + MIN_PASSWORD_LENGTH + " characters and include a letter and a number.");
        }
    }
}
