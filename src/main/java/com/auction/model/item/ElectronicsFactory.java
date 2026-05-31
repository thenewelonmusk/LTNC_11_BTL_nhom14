package com.auction.model.item;


public class ElectronicsFactory implements ItemFactory {
    @Override
    public Item createItem() {
        return new Electronics();
    }
}
