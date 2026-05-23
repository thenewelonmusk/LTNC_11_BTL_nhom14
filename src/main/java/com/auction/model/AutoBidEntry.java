package com.auction.model;

import java.time.LocalDateTime;

public class AutoBidEntry extends Entity {
	private Long auctionId;
	private Long bidderId;
	private double maxBid;
	private double increment;
	private LocalDateTime registeredAt;
	private boolean active;

	public AutoBidEntry() {
		super();
		this.registeredAt = LocalDateTime.now();
		this.active = true;
	}

	public AutoBidEntry(Long auctionId, Long bidderId, double maxBid, double increment) {
		this();
		this.auctionId = auctionId;
		this.bidderId = bidderId;
		this.maxBid = maxBid;
		this.increment = increment;
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

	public double getMaxBid() {
		return maxBid;
	}
	public void setMaxBid(double maxBid) {
		this.maxBid = maxBid;
	}

	public double getIncrement() {
		return increment;
	}
	public void setIncrement(double increment) {
		this.increment = increment;
	}

	public LocalDateTime getRegisteredAt() {
		return registeredAt;
	}
	public void setRegisteredAt(LocalDateTime registeredAt) {
		this.registeredAt = registeredAt;
	}

	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
}
