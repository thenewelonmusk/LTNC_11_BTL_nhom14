package com.auction.model.item;

public class Electronics extends Item {

    public Electronics(int id, String name, String description, double price) {
        super(id, name, description, price);
    }

    @Override
    public String getType() {
        return "Electronics";
    }
}