package com.greenfield.scsp.repository;

import com.greenfield.scsp.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Data-access contract for user accounts.
 * <p>
 * Services depend on this interface rather than on a concrete store
 * (Dependency Inversion Principle), so the in-memory implementation can be
 * swapped for a database implementation without touching business logic.
 */
public interface UserRepository {

    /** Saves a new user or replaces an existing one with the same id. */
    void save(User user);

    /** Finds a user by email, ignoring case. */
    Optional<User> findByEmail(String email);

    /** Finds a user by id. */
    Optional<User> findById(String userId);

    /** Returns every user. */
    List<User> findAll();

    /** Generates the next unique user id. */
    String nextId();
}
