package com.auction.dto;

public class BidRequest {
	private Long auctionId;
	private Long userId;
	private double amount;

	public Long getAuctionId() {
		return auctionId;
	}
	public void setAuctionId(Long auctionId) {
		this.auctionId = auctionId;
	}

	public Long getUserId() {
		return userId;
	} // Getter mới
	public void setUserId(Long userId) {
		this.userId = userId;
	} // Setter mới

	public double getAmount() {
		return amount;
	}
	public void setAmount(double amount) {
		this.amount = amount;
	}
}