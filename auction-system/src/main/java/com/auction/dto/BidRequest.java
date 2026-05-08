package com.auction.dto;

public class BidRequest {
    private Long auctionId;
    private double amount;

    public double getAmount() {return amount;}
    public void setAmount(double amount) {this.amount = amount;}

    public Long getAuctionId() {return auctionId;}
    public void setAuctionId(Long auctionId) {this.auctionId = auctionId;}
}
