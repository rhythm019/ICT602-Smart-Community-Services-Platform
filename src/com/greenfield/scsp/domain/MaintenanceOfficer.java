package com.greenfield.scsp.domain;

/**
 * Field worker who belongs to a {@link MaintenanceTeam} and updates the
 * progress of work assigned to that team.
 */
public class MaintenanceOfficer extends User {

    private MaintenanceTeam team;

    /**
     * Creates a maintenance officer account.
     *
     * @param userId       unique id
     * @param fullName     officer's name
     * @param email        work email
     * @param passwordHash salted password hash
     */
    public MaintenanceOfficer(String userId, String fullName, String email, String passwordHash) {
        super(userId, fullName, email, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.MAINTENANCE_OFFICER;
    }

    public MaintenanceTeam getTeam() { return team; }

    /**
     * Links this officer to a team. Called by {@link MaintenanceTeam#addMember}
     * so both ends of the association stay consistent.
     *
     * @param team the team the officer now belongs to
     */
    void setTeam(MaintenanceTeam team) { this.team = team; }
}
