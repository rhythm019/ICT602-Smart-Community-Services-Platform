package com.greenfield.scsp.domain;

/**
 * A member of the public who reports issues, books facilities and registers
 * for events.
 */
public class Resident extends User {

    private final String phone;
    private final String homeAddress;

    /**
     * Creates a resident account.
     *
     * @param userId       unique id
     * @param fullName     resident's name
     * @param email        login email
     * @param passwordHash salted password hash
     * @param phone        mobile number used for SMS notifications
     * @param homeAddress  residential address within the council area
     */
    public Resident(String userId, String fullName, String email, String passwordHash,
                    String phone, String homeAddress) {
        super(userId, fullName, email, passwordHash);
        this.phone = phone;
        this.homeAddress = homeAddress;
    }

    @Override
    public Role getRole() {
        return Role.RESIDENT;
    }

    public String getPhone() { return phone; }
    public String getHomeAddress() { return homeAddress; }
}
