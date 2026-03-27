package com.auction.model;

public class Bidder extends User{
    public Bidder(int id, String name, String password){
        super(id, name, password);
    }

    public void showRole(){
        System.out.println("Bidder");
    }
}
