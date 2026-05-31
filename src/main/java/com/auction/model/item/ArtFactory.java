package com.auction.model.item;

public class ArtFactory implements ItemFactory{
    @Override
    public Item createItem() {
        return new Art();
    }
}
