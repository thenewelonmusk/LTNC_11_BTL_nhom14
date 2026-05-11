package com.auction.dto;

import com.auction.model.BidTransaction;

public class BidResponse {
    private boolean success;
    private String message;
    private BidTransaction bid;
    private double currentPrice;

    public BidResponse(boolean success, String message, BidTransaction bid, double currentPrice) {
        this.success = success;
        this.message = message;
        this.bid = bid;
        this.currentPrice = currentPrice;
    }

    public boolean isSuccess() {return success;}
    public String getMessage() {return message;}
    public BidTransaction getBid() {return bid;}
    public double getCurrentPrice() {return currentPrice;}
}
