package com.auction.repository;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;

import java.util.List;

public interface AuctionRepository {
	boolean save(Auction auction);
	boolean deleteById(Long id);
	Auction findById(Long id);
	List<Auction> findALl();
	List<Auction> findByStatus(AuctionStatus status);
	List<Auction> findBySellerId(Long sellerId);
	List<Auction> findByItemId(Long itemId);
}
