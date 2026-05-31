package com.auction.model.item;

public class VehicleFactory implements ItemFactory{
    @Override
    public Item createItem() {
        return new Vehicle();
    }
}
