package com.greenfield.scsp.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A crew of maintenance officers that receives work assignments (FR-08).
 * <p>
 * Aggregation: officers are part of a team but exist independently of it.
 */
public class MaintenanceTeam {

    private final String teamId;
    private final String name;
    private final List<MaintenanceOfficer> members = new ArrayList<>();

    /**
     * Creates a team.
     *
     * @param teamId unique id, e.g. "T-ROADS"
     * @param name   display name, e.g. "Roads Crew"
     */
    public MaintenanceTeam(String teamId, String name) {
        this.teamId = teamId;
        this.name = name;
    }

    /**
     * Adds an officer to the team and keeps the back-reference in sync.
     *
     * @param officer officer joining the team
     */
    public void addMember(MaintenanceOfficer officer) {
        if (!members.contains(officer)) {
            members.add(officer);
            officer.setTeam(this);
        }
    }

    public String getTeamId() { return teamId; }
    public String getName() { return name; }
    public List<MaintenanceOfficer> getMembers() { return Collections.unmodifiableList(members); }

    @Override
    public String toString() {
        return teamId + " - " + name;
    }
}
