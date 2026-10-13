package com.greenfield.scsp.repository;

import com.greenfield.scsp.domain.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory implementation of {@link UserRepository} used for the prototype
 * and for automated tests. Data lives only while the program runs.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> usersById = new LinkedHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger(1);

    @Override
    public void save(User user) {
        usersById.put(user.getUserId(), user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        String key = email.trim().toLowerCase();
        return usersById.values().stream()
                .filter(u -> u.getEmail().equals(key))
                .findFirst();
    }

    @Override
    public Optional<User> findById(String userId) {
        return Optional.ofNullable(usersById.get(userId));
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(usersById.values());
    }

    @Override
    public String nextId() {
        // Skip ids already taken by seeded users.
        String id;
        do {
            id = String.format("U-%04d", sequence.getAndIncrement());
        } while (usersById.containsKey(id));
        return id;
    }
}
