package com.auction.model.user;

public class Seller extends User {
    public Seller(Long id, String username, String password){
        super(id, username, password,"SELLER");
    }
}
