package com.auction.service;

import com.auction.dto.AuctionRequest;
import com.auction.dto.AuctionResponse;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;

import java.util.List;

public interface AuctionService {
    AuctionResponse openAuction(AuctionRequest request, Long sellerId);
    AuctionResponse cancelAuction(Long auctionId, Long sellerId);
    AuctionResponse finishAuction(Long auctionId);
    AuctionResponse findAuctionById(Long auctionId);

    /**
     * Cập nhật trạng thái dựa trên thời gian hiện tại
     * (OPEN → RUNNING khi đến giờ bắt đầu, RUNNING → FINISHED khi hết giờ)
     */
    void refreshStatus(Long auctionId);

    List<Auction> getAllAuctions();
    List<Auction> getAuctionsByStatus(AuctionStatus status);
    List<Auction> getAuctionsBySeller(Long sellerId);
}
