package com.auction.model;

import com.auction.model.user.Bidder;

import java.time.LocalDateTime;

public class BidTransaction {
    private double amount;
    private Bidder bidder;
    private LocalDateTime time;

    public BidTransaction(double amount, Bidder bidder) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid bid amount");
        }
        this.amount = amount;
        this.bidder = bidder;
        this.time = LocalDateTime.now();
    }

    public double getAmount() {
        return amount;
    }

    public Bidder getBidder() {
        return bidder;
    }

    public LocalDateTime getTime() {
        return time;
    }
}