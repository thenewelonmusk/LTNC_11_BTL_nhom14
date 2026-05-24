package com.auction.dto;

/**
 * Request từ client để đăng ký một Auto-Bid. - maxBid: giá tối đa người dùng
 * chấp nhận trả. - increment: bước giá mỗi lần hệ thống tự trả thay.
 */
public class AutoBidRequest {
	private Long auctionId;
	private Long bidderId;
	private double maxBid;
	private double increment;

	public AutoBidRequest() {
	}

	public AutoBidRequest(Long auctionId, Long bidderId, double maxBid, double increment) {
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
}
