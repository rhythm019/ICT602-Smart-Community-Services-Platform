package com.greenfield.scsp.domain;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Abstract base class for every person who can sign in to the platform.
 * <p>
 * Holds identity, credential and lock-out state shared by all users.
 * Concrete subclasses ({@link Resident}, {@link CouncilEmployee},
 * {@link MaintenanceOfficer}, {@link Administrator}) supply their role,
 * which demonstrates inheritance and the Liskov Substitution Principle:
 * any subclass can be used wherever a {@code User} is expected.
 */
public abstract class User {

    /** Number of consecutive failed logins before the account locks (NFR-01). */
    public static final int MAX_FAILED_ATTEMPTS = 5;

    /** How long a locked account stays locked. */
    public static final Duration LOCKOUT_PERIOD = Duration.ofMinutes(15);

    private final String userId;
    private final String fullName;
    private final String email;
    private String passwordHash;
    private boolean active = true;
    private int failedLoginAttempts;
    private LocalDateTime lockedUntil;

    /**
     * Creates a user.
     *
     * @param userId       unique identifier, e.g. "U-0001"
     * @param fullName     display name
     * @param email        login email (unique, case-insensitive)
     * @param passwordHash salted hash produced by a PasswordHasher
     */
    protected User(String userId, String fullName, String email, String passwordHash) {
        this.userId = Objects.requireNonNull(userId);
        this.fullName = Objects.requireNonNull(fullName);
        this.email = Objects.requireNonNull(email).toLowerCase();
        this.passwordHash = Objects.requireNonNull(passwordHash);
    }

    /**
     * Returns the role of this user. Each subclass decides its own role.
     *
     * @return the user's role
     */
    public abstract Role getRole();

    /**
     * Checks whether this user may perform an action.
     *
     * @param permission the action being attempted
     * @return true when the account is active and its role grants the permission
     */
    public boolean hasPermission(Permission permission) {
        return active && getRole().grants(permission);
    }

    /**
     * Records a failed login and locks the account once the limit is reached.
     *
     * @param now current time (passed in so tests can control the clock)
     */
    public void registerFailedLogin(LocalDateTime now) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = now.plus(LOCKOUT_PERIOD);
        }
    }

    /** Clears the failed-login counter after a successful sign-in. */
    public void resetFailedLogins() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }

    /**
     * Tells whether the account is currently locked out.
     *
     * @param now current time
     * @return true while the lock-out period has not expired
     */
    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public String getUserId() { return userId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }

    @Override
    public String toString() {
        return String.format("%s  %-22s %-32s %-20s %s",
                userId, fullName, email, getRole(), active ? "Active" : "Disabled");
    }
}
