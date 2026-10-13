package com.greenfield.scsp.repository;

import com.greenfield.scsp.domain.MaintenanceTeam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory implementation of {@link MaintenanceTeamRepository}.
 */
public class InMemoryMaintenanceTeamRepository implements MaintenanceTeamRepository {

    private final Map<String, MaintenanceTeam> teams = new LinkedHashMap<>();

    @Override
    public void save(MaintenanceTeam team) {
        teams.put(team.getTeamId().toUpperCase(), team);
    }

    @Override
    public Optional<MaintenanceTeam> findById(String teamId) {
        if (teamId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(teams.get(teamId.trim().toUpperCase()));
    }

    @Override
    public List<MaintenanceTeam> findAll() {
        return new ArrayList<>(teams.values());
    }
}
