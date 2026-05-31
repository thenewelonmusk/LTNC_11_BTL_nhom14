package com.auction.model.item;

import com.auction.dto.ItemRequest;

public class ItemFactory {
	private ItemFactory() {
	}

	public static Item create(ItemRequest req) {
		if (req == null || req.getType() == null) {
			throw new IllegalArgumentException("Dữ liệu yêu cầu hoặc loại sản phẩm không được để trống");
		}

		Item item;
		String type = req.getType().toUpperCase();

		switch (type) {
			case "ELECTRONICS" :
				item = new Electronics(req.getDeviceBrand(), req.getWarrantyMonths());
				break;
			case "ART" :
				item = new Art(req.getArtist(), req.getYear());
				break;
			case "VEHICLE" :
				item = new Vehicle(req.getVehicleBrand(), req.getMileage());
				break;
			default :
				throw new IllegalArgumentException("Loại sản phẩm không xác định: " + type);
		}

		item.setId(req.getItemId());
		item.setName(req.getName());
		item.setDescription(req.getDescription());
		item.setStartingPrice(req.getStartingPrice());
		item.setCurrentPrice(req.getCurrentPrice());
		item.setSellerId(req.getSellerId());

		return item;
	}
}
