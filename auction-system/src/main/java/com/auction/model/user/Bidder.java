package com.auction.model.user;

public class Bidder extends User {
    public Bidder(int id, String uesername, String password){
        super(id, username, password);
    }
    @Override
    public void showRole(){
        System.out.println("Bidder");
    }
}
