package com.auction.model;

public class Admin extends User{
    public Admin(int id, String name, String password){
        super(id, name, password);
    }

    public void showRole(){
        System.out.println("ADMIN");
    }
}
