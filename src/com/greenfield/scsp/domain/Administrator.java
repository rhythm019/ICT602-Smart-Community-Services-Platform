package com.greenfield.scsp.domain;

/**
 * System administrator who manages user accounts, roles and the audit log.
 */
public class Administrator extends User {

    /**
     * Creates an administrator account.
     *
     * @param userId       unique id
     * @param fullName     administrator's name
     * @param email        work email
     * @param passwordHash salted password hash
     */
    public Administrator(String userId, String fullName, String email, String passwordHash) {
        super(userId, fullName, email, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.ADMINISTRATOR;
    }
}
