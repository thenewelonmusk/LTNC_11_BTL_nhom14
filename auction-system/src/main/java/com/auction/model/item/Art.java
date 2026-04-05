package com.auction.model.item;

public class Art extends Item {

    public Art(int id, String name, String description, double price) {
        super(id, name, description, price);
    }

    @Override
    public String getType() {
        return "Art";
    }
}