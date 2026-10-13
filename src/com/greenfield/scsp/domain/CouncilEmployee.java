package com.greenfield.scsp.domain;

/**
 * Council staff member who reviews, prioritises and assigns service requests.
 */
public class CouncilEmployee extends User {

    private final String department;

    /**
     * Creates a council employee account.
     *
     * @param userId       unique id
     * @param fullName     employee's name
     * @param email        work email
     * @param passwordHash salted password hash
     * @param department   council department, e.g. "Infrastructure"
     */
    public CouncilEmployee(String userId, String fullName, String email,
                           String passwordHash, String department) {
        super(userId, fullName, email, passwordHash);
        this.department = department;
    }

    @Override
    public Role getRole() {
        return Role.COUNCIL_EMPLOYEE;
    }

    public String getDepartment() { return department; }
}
