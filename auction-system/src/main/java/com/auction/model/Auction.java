package com.auction.model;

import java.time.LocalDateTime;

public class Auction extends Entity {
   private Long itemId;
   private Long sellerId;
   private double startingPrice;
   private double currentPrice;
   private LocalDateTime startTime;
   private LocalDateTime endTime;
   private AuctionStatus status;
   private Long winnerId;

   public Auction() {
      super();
      this.status = AuctionStatus.OPEN;
   }

   public Long getItemId() {return itemId;}
   public void setItemId(Long itemId) {this.itemId = itemId;}

   public Long getSellerId() {return sellerId;}
   public void setSellerId(Long sellerId) {this.sellerId = sellerId;}

   public double getStartingPrice() {return startingPrice;}
   public void setStartingPrice(double startingPrice) {this.startingPrice = startingPrice;}

   public double getCurrentPrice() {return currentPrice;}
   public void setCurrentPrice(double currentPrice) {this.currentPrice = currentPrice;}

   public LocalDateTime getStartTime() {return startTime;}
   public void setStartTime(LocalDateTime startTime) {this.startTime = startTime;}

   public LocalDateTime getEndTime() {return endTime;}
   public void setEndTime(LocalDateTime endTime) {this.endTime = endTime;}

   public AuctionStatus getStatus() {return status;}
   public void setStatus(AuctionStatus status) {this.status = status;}

   public Long getWinnerId() {return winnerId;}
   public void setWinnerId(Long winnerId) {this.winnerId = winnerId;}
}