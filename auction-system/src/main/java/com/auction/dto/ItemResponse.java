package com.auction.dto;

import com.auction.model.item.Item;

public class ItemResponse {
    private boolean success;
    private String message;
    private Item item;

    public ItemResponse(boolean success, String message, Item item) {
        this.success = success;
        this.message = message;
        this.item = item;
    }

    public boolean isSuccess() {return success;}
    public String getMessage() {return message;}
    public Item getItem() {return item;}
}
