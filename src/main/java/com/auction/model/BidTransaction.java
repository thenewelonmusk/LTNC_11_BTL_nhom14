package com.auction.model;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
	private Long auctionId;
	private Long bidderId;
	private double amount;
	private LocalDateTime bidTime;

	public BidTransaction() {
		super();
		this.bidTime = LocalDateTime.now();
	}

	public Long getAuctionId() {
		return auctionId;
	}
	public void setAuctionId(Long auctionId) {
		this.auctionId = auctionId;
	}

	public Long getBidderId() {
		return bidderId;
	}
	public void setBidderId(Long bidderId) {
		this.bidderId = bidderId;
	}

	public double getAmount() {
		return amount;
	}
	public void setAmount(double amount) {
		this.amount = amount;
	}

	public LocalDateTime getBidTime() {
		return bidTime;
	}
	public void setBidTime(LocalDateTime bidTime) {
		this.bidTime = bidTime;
	}
}
