package com.auction.model.user;

public class Admin extends User {
    public Admin(int id, String username, String password){
        super(id, username, password);
    }
    @Override
    public void showRole(){
        System.out.println("Admin");
    }
}
