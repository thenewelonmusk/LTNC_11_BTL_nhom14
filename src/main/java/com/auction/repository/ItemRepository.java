package com.auction.repository;

import com.auction.model.item.Item;

import java.util.List;

public interface ItemRepository {
	boolean save(Item item);
	boolean deleteById(Long itemId);
	Item findById(Long itemId);

	List<Item> findAll();
	List<Item> findBySeller(Long sellerId);
	List<Item> findByType(String type);
}
