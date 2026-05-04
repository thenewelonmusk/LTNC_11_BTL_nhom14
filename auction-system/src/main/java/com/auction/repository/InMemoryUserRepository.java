package com.auction.repository;

import com.auction.model.user.User;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory implementation of UserRepository using HashMap for storage.
 * Provides CRUD operations for User entities.
 */
public class InMemoryUserRepository implements UserRepository {

    private final Map<Integer, User> userMap = new HashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(0);

    /**
     * Saves a user (insert or update).
     * If the user has no ID, a new one is generated.
     *
     * @param user the user to save
     * @return true if save was successful
     */
    @Override
    public boolean saveUser(User user) {
        if (user == null) {
            return false;
        }

        User userToSave = user;
        if (user.getId() == 0) {
            int newId = idGenerator.incrementAndGet();
            userToSave = new User(
                newId,
                user.getUsername(),
                user.getPassword(),
                user.getRole()
            );
        }

        userMap.put(userToSave.getId(), userToSave);
        return true;
    }

    /**
     * Finds a user by username.
     *
     * @param username the username to search for
     * @return the user if found, null otherwise
     */
    @Override
    public User findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        return userMap.values().stream()
            .filter(user -> user.getUsername().equals(username))
            .findFirst()
            .orElse(null);
    }

    /**
     * Finds a user by ID.
     *
     * @param id the user ID
     * @return the user if found, null otherwise
     */
    public User findById(int id) {
        return userMap.get(id);
    }
}