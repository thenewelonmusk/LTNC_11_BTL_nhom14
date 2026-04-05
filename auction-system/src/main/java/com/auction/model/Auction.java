package com.auction.model;

import com.auction.model.item.Item;
import com.auction.model.user.Bidder;
import com.auction.model.user.Seller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private Item item;
    private Seller seller;
    private List<BidTransaction> bids;
    private double currentPrice;
    private Bidder highestBidder;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;

    public Auction(int id, Item item, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.item = item;
        this.seller = seller;
        this.startTime = startTime;
        this.endTime = endTime;
        this.bids = new ArrayList<>();
        this.currentPrice = item.getStartingPrice();
        this.status = AuctionStatus.OPEN;
    }

    public synchronized boolean placeBid(BidTransaction bid) {
        if (status != AuctionStatus.RUNNING) return false;
        if (bid.getAmount() <= currentPrice) return false;

        bids.add(bid);
        currentPrice = bid.getAmount();
        highestBidder = bid.getBidder();
        return true;
    }

    public void start() {
        this.status = AuctionStatus.RUNNING;
    }

    public void finish() {
        this.status = AuctionStatus.FINISHED;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public Bidder getHighestBidder() {
        return highestBidder;
    }
}