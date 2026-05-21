package com.auction.dto;

public class ItemResponse {
	private boolean success;
	private String message;
	private ItemRequest itemData;

	public ItemResponse(boolean success, String message, ItemRequest itemData) {
		this.success = success;
		this.message = message;
		this.itemData = itemData;
	}

	public boolean isSuccess() {
		return success;
	}
	public String getMessage() {
		return message;
	}
	public ItemRequest getItemData() {
		return itemData;
	}
}