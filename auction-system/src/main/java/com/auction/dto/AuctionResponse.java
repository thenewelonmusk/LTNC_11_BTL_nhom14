package com.auction.dto;

import com.auction.model.Auction;

public class AuctionResponse {
	private boolean success;
	private String message;
	private Auction auction;

	public AuctionResponse(boolean success, String message, Auction auction) {
		this.success = success;
		this.message = message;
		this.auction = auction;
	}

	public boolean isSuccess() {
		return success;
	}
	public String getMessage() {
		return message;
	}
	public Auction getAuction() {
		return auction;
	}
}
