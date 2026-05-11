package com.auction.service.impl;

import com.auction.dto.ItemRequest;
import com.auction.dto.ItemResponse;
import com.auction.model.item.Item;
import com.auction.model.item.ItemFactory;
import com.auction.repository.ItemRepository;
import com.auction.service.ItemService;

import java.util.List;

public class ItemServiceImpl implements ItemService {
    // Error messages
    private static final String ERROR_INVALID_REQUEST = "Yêu cầu không hợp lệ.";
    private static final String ERROR_NAME_REQUIRED = "Tên sản phẩm bắt buộc.";
    private static final String ERROR_NAME_TOO_LONG = "Tên sản phẩm quá dài (tối đa 200).";
    private static final String ERROR_ITEM_NOT_FOUND = "Không tìm thấy sản phẩm.";
    private static final String ERROR_NOT_OWNER = "Bạn không phải chủ sản phẩm này.";
    private static final String ERROR_SAVE_FAILED = "Lỗi hệ thống, không thể lưu.";
    private static final String ERROR_DELETE_FAILED = "Không thể xóa sản phẩm.";

    // Success messages
    private static final String SUCCESS_CREATE = "Tạo sản phẩm thành công.";
    private static final String SUCCESS_UPDATE = "Cập nhật thành công.";
    private static final String SUCCESS_DELETE = "Xóa sản phẩm thành công.";
    private static final String SUCCESS_FOUND = "OK.";

    private final ItemRepository itemRepository;

    public ItemServiceImpl(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Override
    public ItemResponse createItem(ItemRequest request, Long sellerId) {
        String validationError = validateRequest(request);
        if (validationError != null) {
            return new ItemResponse(false,validationError,null);
        }

        request.setSellerId(sellerId);
        Item item = ItemFactory.create(request);
        boolean ok = itemRepository.save(item);
        if (!ok) {
            return new ItemResponse(false,ERROR_SAVE_FAILED,null);
        }
        return new ItemResponse(true,SUCCESS_CREATE,item);
    }


    // chỉ cho phép update description
    @Override
    public ItemResponse updateItem(Long itemId, ItemRequest request, Long sellerId) {
        Item existing = itemRepository.findById(itemId);
        if (existing == null) {
            return new ItemResponse(false,ERROR_ITEM_NOT_FOUND,null);
        }
        if (!sellerId.equals(existing.getSellerId())) {
            return new ItemResponse(false,ERROR_NOT_OWNER,null);
        }
        String validationError = validateRequest(request);
        if (validationError != null) {
            return new ItemResponse(false,validationError, null);
        }

        existing.setDescription(request.getDescription());

        boolean ok = itemRepository.save(existing);
        if (!ok) {
            return new ItemResponse(false,ERROR_SAVE_FAILED,null);
        }
        return new ItemResponse(true,SUCCESS_UPDATE,existing);
    }

    @Override
    public ItemResponse deleteItem(Long itemId, Long sellerId) {
        Item existing = itemRepository.findById(itemId);
        if (existing == null) {
            return new ItemResponse(false,ERROR_ITEM_NOT_FOUND,null);
        }
        if (!sellerId.equals(existing.getSellerId())) {
            return new ItemResponse(false,ERROR_NOT_OWNER,null);
        }
        boolean ok = itemRepository.deleteById(itemId);
        if (!ok) {
            return new ItemResponse(false,ERROR_DELETE_FAILED,null);
        }
        return new ItemResponse(true,SUCCESS_DELETE,existing);
    }

    @Override
    public ItemResponse findItemById(Long itemId) {
        Item item = itemRepository.findById(itemId);
        if (item == null) {
            return new ItemResponse(false,ERROR_ITEM_NOT_FOUND,null);
        }
        return new ItemResponse(true,SUCCESS_FOUND,item);
    }

    @Override
    public List<Item> getAll() {
        return itemRepository.findAll();
    }

    @Override
    public List<Item> findByType(String type) {
        if (type == null || type.isBlank()) {
            return List.of();
        }
        return itemRepository.findByType(type.toUpperCase());
    }

    @Override
    public List<Item> findBySeller(Long sellerId) {
        return itemRepository.findBySeller(sellerId);
    }

    public String validateRequest(ItemRequest req) {
        if (req == null) {
            return ERROR_INVALID_REQUEST;
        }
        if (req.getName() == null || req.getName().isBlank()) {
            return ERROR_NAME_REQUIRED;
        }
        if (req.getName().length() > 200) {
            return ERROR_NAME_TOO_LONG;
        }
        return null;
    }
}
