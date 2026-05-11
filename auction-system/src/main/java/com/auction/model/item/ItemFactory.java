package com.auction.model.item;

import com.auction.dto.ItemRequest;

public class ItemFactory {
    private ItemFactory() {}

    public static Item create(ItemRequest req) {
        Item item;

        switch (req.getType().toUpperCase()) {
            case "ELECTRONICS":
                item = new Electronics(req.getDeviceBrand(),req.getWarrantyMonths());
                break;
            case "ART":
                item = new Art(req.getArtist(),req.getYear());
                break;
            case "VEHICLE":
                item = new Vehicle(req.getVehicleBrand(),req.getMileage());
                break;
            default:
                throw new IllegalArgumentException(
                        "Loại sản phẩm không xác định: " + req.getType()
                );
        }

        item.setName(req.getName());
        item.setDescription(req.getDescription());
        item.setStartingPrice(req.getStartingPrice());
        item.setSellerId(req.getSellerId());

        return item;
    }
}
