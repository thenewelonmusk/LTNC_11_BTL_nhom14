package com.auction.repository;

import com.auction.model.BidTransaction;

import java.util.List;

public interface BidRepository {
	boolean save(BidTransaction bid);
	BidTransaction findById(Long id);
	List<BidTransaction> findByAuctionId(Long auctionId);
	List<BidTransaction> findByBidderId(Long bidderId);
	BidTransaction findHighestBidByAuction(Long auctionId); // bid cao nhất của 1 phiên
	List<BidTransaction> findAll();
}
