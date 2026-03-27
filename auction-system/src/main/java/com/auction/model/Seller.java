package com.auction.model;

public class Seller extends User{
    public Seller(int id, String name, String password){
        super(id, name, password);
    }

    public void showRole(){
        System.out.println("Seller");
    }
}
