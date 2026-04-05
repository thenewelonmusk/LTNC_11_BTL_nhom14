package com.auction.model.item;

import com.auction.model.Entity;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startingPrice;

    public Item(int id, String name, String description, double startingPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public abstract String getType();
}