package com.auction.model.user;

import com.auction.model.Entity;

public class User extends Entity {
    protected String username;
    protected String password;
    protected String role;

    public User(int id, String username, String password,String role) {
        super(id);
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }
    public String getPassword() { return password; }

    public String getRole() { return role; }
}