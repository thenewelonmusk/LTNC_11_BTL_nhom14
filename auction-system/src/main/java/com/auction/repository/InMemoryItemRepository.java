package com.auction.repository;

import com.auction.model.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryItemRepository implements ItemRepository {
	private final Map<Long, Item> itemMap = new HashMap<>();
	private final AtomicLong idGenerator = new AtomicLong(0);

	@Override
	public boolean save(Item item) {
		if (item == null) {
			return false;
		}
		if (item.getId() == null) {
			long newId = idGenerator.incrementAndGet();
			item.setId(newId);
		}
		itemMap.put(item.getId(), item);
		return true;
	}

	@Override
	public boolean deleteById(Long itemId) {
		if (itemId == null) {
			return false;
		}
		return itemMap.remove(itemId) != null;
	}

	@Override
	public Item findById(Long itemId) {
		if (itemId == null) {
			return null;
		}
		return itemMap.get(itemId);
	}

	@Override
	public List<Item> findAll() {
		return new ArrayList<>(itemMap.values());
	}

	@Override
	public List<Item> findBySeller(Long sellerId) {
		List<Item> result = new ArrayList<>();
		if (sellerId == null) {
			return result;
		}
		for (Item item : itemMap.values()) {
			if (sellerId.equals(item.getSellerId())) {
				result.add(item);
			}
		}
		return result;
	}

	@Override
	public List<Item> findByType(String type) {
		List<Item> result = new ArrayList<>();
		if (type == null) {
			return result;
		}
		for (Item item : itemMap.values()) {
			if (type.equals(item.getType().toUpperCase())) {
				result.add(item);
			}
		}
		return result;
	}
}
