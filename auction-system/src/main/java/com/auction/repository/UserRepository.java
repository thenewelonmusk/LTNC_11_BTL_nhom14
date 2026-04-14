package com.auction.repository;
import com.auction.model.user.User;

public interface UserRepository {
    User findByUsername(String username);

    boolean saveUser(User user);
}
