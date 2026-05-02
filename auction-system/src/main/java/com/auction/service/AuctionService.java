package com.auction.service;

import com.auction.model.Auction;
import com.auction.model.BidTransaction;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static java.time.LocalDateTime.now;

public class AuctionService {
    private Map<Long, Auction> auctionMap;
    private static final long SNIPING_THRESHOLD = 30;
    private static final long EXTENSION_SECONDS = 60;

    public AuctionService(Map<Integer, Auction> auctionMap) {
        this.auctionMap = new HashMap<>();
    }

    public boolean addAuction(Auction auction) {
        if (auctionMap.containsKey(auction.getId())) {
            return false;
        }
        auctionMap.put(auction.getId(),auction);
        return true;
    }

    public boolean deleteAuction(int id) {
        return auctionMap.remove(id) != null;
    }

    public Auction findAuctionById(int id) {
        return auctionMap.get(id);
    }

    public boolean updateAuction(Long id, Auction newAuctionData) {
        if (auctionMap.containsKey(id)) {
            auctionMap.put(id,newAuctionData);
            return true;
        } return false;
    }

    public boolean placeBid(int auctionId, BidTransaction bid) {
        Auction auction = findAuctionById(auctionId);
        if (auction == null) {
            return false;
        }

        boolean isBidSuccessful = auction.placeBid(bid);

        if (isBidSuccessful) {
            LocalDateTime now = now();
            LocalDateTime end = auction.getEndTime();

            long secondsRemaining = Duration.between(now,end).getSeconds();

            if (secondsRemaining > 0 && secondsRemaining <= SNIPING_THRESHOLD) {
                LocalDateTime newBidEnd = end.plusSeconds(EXTENSION_SECONDS);
                auction.setEndTime(newBidEnd);
            }
        }

        return isBidSuccessful;
    }
}
