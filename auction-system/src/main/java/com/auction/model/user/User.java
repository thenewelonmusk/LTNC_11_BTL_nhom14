package com.auction.model.user;

import com.auction.model.Entity;

public abstract class User extends Entity {
    protected String username;
    protected String password;

    public User(int id, String username, String password) {
        super(id);
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public abstract void showRole();
}