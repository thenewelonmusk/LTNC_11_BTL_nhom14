package com.auction.dto;

import com.auction.model.AutoBidEntry;

public class AutoBidResponse {
	private boolean success;
	private String message;
	private AutoBidEntry entry;

	public AutoBidResponse(boolean success, String message, AutoBidEntry entry) {
		this.success = success;
		this.message = message;
		this.entry = entry;
	}

	public boolean isSuccess() {
		return success;
	}
	public String getMessage() {
		return message;
	}
	public AutoBidEntry getEntry() {
		return entry;
	}
}