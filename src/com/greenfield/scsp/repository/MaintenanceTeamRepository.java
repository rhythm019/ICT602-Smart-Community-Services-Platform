package com.greenfield.scsp.repository;

import com.greenfield.scsp.domain.MaintenanceTeam;

import java.util.List;
import java.util.Optional;

/**
 * Data-access contract for maintenance teams.
 */
public interface MaintenanceTeamRepository {

    /** Saves a team. */
    void save(MaintenanceTeam team);

    /** Finds a team by id, ignoring case. */
    Optional<MaintenanceTeam> findById(String teamId);

    /** Returns every team. */
    List<MaintenanceTeam> findAll();
}
