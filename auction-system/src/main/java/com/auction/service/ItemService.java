package com.auction.service;

import com.auction.dto.ItemRequest;
import com.auction.dto.ItemResponse;
import com.auction.model.item.Item;

import java.util.List;

public interface ItemService {
    ItemResponse createItem(ItemRequest request, Long sellerId);
    ItemResponse updateItem(Long itemId, ItemRequest request, Long sellerId);
//    ItemResponse saveItem(ItemRequest request, Long sellerId);
    ItemResponse deleteItem(Long itemId, Long sellerId);

//    List<Item> getAll();
    List<Item> findByType(String type);
    List<Item> findBySeller(Long sellerId);
}
