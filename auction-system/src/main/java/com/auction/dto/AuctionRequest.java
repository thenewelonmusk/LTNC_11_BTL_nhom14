package com.auction.dto;

import java.time.LocalDateTime;

public class AuctionRequest {
    private Long itemId;
    private double startingPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Long getItemId() {return itemId;}
    public void setItemId(Long itemId) {this.itemId = itemId;}

    public double getStartingPrice() {return startingPrice;}
    public void setStartingPrice(double startingPrice) {this.startingPrice = startingPrice;}

    public LocalDateTime getStartTime() {return startTime;}
    public void setStartTime(LocalDateTime startTime) {this.startTime = startTime;}

    public LocalDateTime getEndTime() {return endTime;}
    public void setEndTime(LocalDateTime endTime) {this.endTime = endTime;}
}
