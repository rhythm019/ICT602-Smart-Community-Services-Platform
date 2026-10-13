package com.greenfield.scsp.repository;

import com.greenfield.scsp.domain.RequestStatus;
import com.greenfield.scsp.domain.ServiceRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory implementation of {@link ServiceRequestRepository}.
 */
public class InMemoryServiceRequestRepository implements ServiceRequestRepository {

    private final Map<String, ServiceRequest> requests = new LinkedHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(1001);

    @Override
    public void save(ServiceRequest request) {
        requests.put(request.getRequestId(), request);
    }

    @Override
    public Optional<ServiceRequest> findById(String requestId) {
        if (requestId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(requests.get(requestId.trim().toUpperCase()));
    }

    @Override
    public List<ServiceRequest> findByReporter(String residentId) {
        return requests.values().stream()
                .filter(r -> r.getReporter().getUserId().equals(residentId))
                .toList();
    }

    @Override
    public List<ServiceRequest> findByStatus(RequestStatus status) {
        return requests.values().stream()
                .filter(r -> r.getStatus() == status)
                .toList();
    }

    @Override
    public List<ServiceRequest> findByTeam(String teamId) {
        return requests.values().stream()
                .filter(r -> r.getAssignedTeam() != null
                        && r.getAssignedTeam().getTeamId().equals(teamId))
                .toList();
    }

    @Override
    public List<ServiceRequest> findAll() {
        return new ArrayList<>(requests.values());
    }

    @Override
    public String nextId() {
        return "SR-" + sequence.getAndIncrement();
    }
}
