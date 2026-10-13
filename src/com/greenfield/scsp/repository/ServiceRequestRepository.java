package com.greenfield.scsp.repository;

import com.greenfield.scsp.domain.RequestStatus;
import com.greenfield.scsp.domain.ServiceRequest;

import java.util.List;
import java.util.Optional;

/**
 * Data-access contract for service requests (Repository pattern).
 */
public interface ServiceRequestRepository {

    /** Saves a new request or replaces an existing one. */
    void save(ServiceRequest request);

    /** Finds a request by its id. */
    Optional<ServiceRequest> findById(String requestId);

    /** Returns all requests reported by a resident. */
    List<ServiceRequest> findByReporter(String residentId);

    /** Returns all requests in a given status. */
    List<ServiceRequest> findByStatus(RequestStatus status);

    /** Returns all requests assigned to a maintenance team. */
    List<ServiceRequest> findByTeam(String teamId);

    /** Returns every request. */
    List<ServiceRequest> findAll();

    /** Generates the next unique request id. */
    String nextId();
}
