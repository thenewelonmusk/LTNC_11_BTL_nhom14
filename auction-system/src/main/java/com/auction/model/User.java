package com.auction.model;

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