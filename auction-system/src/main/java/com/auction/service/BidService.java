package com.auction.service;

import com.auction.dto.BidRequest;
import com.auction.dto.BidResponse;
import com.auction.model.BidTransaction;

import java.util.List;

public interface BidService {
	BidResponse placeBid(BidRequest request, Long bidderId);
	List<BidTransaction> getBidsByAuction(Long auctionId);
	List<BidTransaction> getBidsByBidder(Long bidderId);
	BidTransaction getHighestBid(Long auctionId);
}
