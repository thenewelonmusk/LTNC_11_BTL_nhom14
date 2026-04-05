package com.auction.model.item;

public class Vehicle extends Item {

    public Vehicle(int id, String name, String description, double price) {
        super(id, name, description, price);
    }

    @Override
    public String getType() {
        return "Vehicle";
    }
}