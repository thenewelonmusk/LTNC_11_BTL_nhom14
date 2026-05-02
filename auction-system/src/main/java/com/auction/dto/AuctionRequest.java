package com.auction.dto;

public class AuctionRequest {
    private Long itemId;
    private double startingPrice;
    private double startTime;
    private double endTime;

    public Long getItemId() {return itemId;}
    public void setItemId(Long itemId) {this.itemId = itemId;}

    public double getStartingPrice() {return startingPrice;}
    public void setStartingPrice(double startingPrice) {this.startingPrice = startingPrice;}

    public double getStartTime() {return startTime;}
    public void setStartTime(double startTime) {this.startTime = startTime;}

    public double getEndTime() {return endTime;}
    public void setEndTime(double endTime) {this.endTime = endTime;}
}
