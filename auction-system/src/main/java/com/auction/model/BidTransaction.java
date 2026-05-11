package com.auction.model;

import com.auction.model.user.Bidder;

import java.time.LocalDateTime;

public class BidTransaction extends Entity{
    private Long auctionId;
    private Long bidderId;
    private double amount;
    private LocalDateTime bidTime;
    private boolean autoBid;

    public BidTransaction() {
        super();
        this.bidTime = LocalDateTime.now();
        this.autoBid = false;
    }

    public Long getAuctionId() {return auctionId;}
    public void setAuctionId(Long auctionId) {this.auctionId = auctionId;}

    public Long getBidderId() {return bidderId;}
    public void setBidderId(Long bidderId) {this.bidderId = bidderId;}

    public double getAmount() {return amount;}
    public void setAmount(double amount) {this.amount = amount;}

    public LocalDateTime getBidTime() {return bidTime;}
    public void setBidTime(LocalDateTime bidTime) {this.bidTime = bidTime;}

    public boolean isAutoBid() {return autoBid;}
    public void setAutoBid(boolean autoBid) {this.autoBid = autoBid;}
}