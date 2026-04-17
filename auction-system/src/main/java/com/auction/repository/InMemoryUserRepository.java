package com.auction.repository;
import com.auction.model.user.User;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryUserRepository implements UserRepository {

    private Map<Integer, User> userMap = new HashMap<>();
    private AtomicInteger idGenerator = new AtomicInteger(0);

    // SAVE (insert + update)
    @Override
    public boolean saveUser(User user) {

        // nếu user mới → tạo id
        if (user.getId() == 0) {
            int newId = idGenerator.incrementAndGet();

            user = new User(
                newId,
                user.getUsername(),
                user.getPassword(),
                user.getRole()
            );
        }

        // insert hoặc update đều dùng put
        userMap.put(user.getId(), user);
        return true;
    }

    // FIND BY USERNAME
    @Override
    public User findByUsername(String username) {
        for (User user : userMap.values()) {
            if (user.getUsername().equals(username)) {
                return user;
            }
        }
        return null;
    }

    // FIND BY ID
}