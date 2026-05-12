package com.auction.model.item;

import com.auction.model.Entity;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startingPrice;
    protected double currentPrice;
    protected Long sellerId;

    protected Item() {super();}

    public Item(Long id, String name, String description, double startingPrice, Long sellerId) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice;
    }

    public String getName() {return name;}
    public String getDescription() {return description;}
    public double getStartingPrice() {return startingPrice;}
    public double getCurrentPrice() {return currentPrice;}
    public Long getSellerId() {return sellerId;}

    public void setName(String name) {this.name = name;}
    public void setDescription(String description) {this.description = description;}
    public void setStartingPrice(double startingPrice) {this.startingPrice = startingPrice;}
    public void setCurrentPrice(double currentPrice) {this.currentPrice = currentPrice;}
    public void setSellerId(Long sellerId) {this.sellerId = sellerId;}

    public abstract String getType();
    public abstract void getInfo();
}