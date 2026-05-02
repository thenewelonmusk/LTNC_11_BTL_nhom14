package com.auction.model.user;

public class Admin extends User {
    public Admin(Long id, String username, String password){
        super(id, username, password,"ADMIN");
    }
}
