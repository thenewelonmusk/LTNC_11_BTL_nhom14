package com.auction.service.impl;

import com.auction.dao.ItemDAO;
import com.auction.dto.ItemRequest;
import com.auction.dto.ItemResponse;
import com.auction.model.item.*;
import com.auction.service.ItemService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemServiceImpl implements ItemService {
	private static final String ERROR_INVALID_REQUEST = "Yêu cầu không hợp lệ.";
	private static final String ERROR_NAME_REQUIRED = "Tên sản phẩm bắt buộc.";
	private static final String ERROR_NAME_TOO_LONG = "Tên sản phẩm quá dài (tối đa 200).";
	private static final String ERROR_ITEM_NOT_FOUND = "Không tìm thấy sản phẩm.";
	private static final String ERROR_NOT_OWNER = "Bạn không phải chủ sản phẩm này.";
	private static final String ERROR_SAVE_FAILED = "Lỗi hệ thống, không thể lưu.";
	private static final String ERROR_DELETE_FAILED = "Không thể xóa sản phẩm.";
	private static final String ERROR_INVALID_ITEM_TYPE = "Loại sản phẩm không hợp lệ";

	private static final String SUCCESS_CREATE = "Tạo sản phẩm thành công.";
	private static final String SUCCESS_UPDATE = "Cập nhật thành công.";
	private static final String SUCCESS_DELETE = "Xóa sản phẩm thành công.";

	private final ItemDAO itemDAO;

	public static final Map<String, ItemFactory> factoryRegistry = new HashMap<>();

	static {
		factoryRegistry.put("ELECTRONICS", new ElectronicsFactory());
		factoryRegistry.put("ART", new ArtFactory());
		factoryRegistry.put("VEHICLE", new VehicleFactory());
	}

	public ItemServiceImpl(ItemDAO itemDAO) {
		this.itemDAO = itemDAO;
	}

	@Override
	public ItemResponse createItem(ItemRequest request, Long sellerId) {
		String validationError = validateRequest(request);
		if (validationError != null) {
			return new ItemResponse(false, validationError, null);
		}

		try {
			request.setSellerId(sellerId);

			ItemFactory factory = factoryRegistry.get(request.getType().trim().toUpperCase());
			if (factory == null) {
				return new ItemResponse(false, ERROR_INVALID_ITEM_TYPE, null);
			}

			Item item = factory.createItem();

			item.setName(request.getName());
			item.setDescription(request.getDescription());
			item.setStartingPrice(request.getStartingPrice());
			item.setCurrentPrice(request.getStartingPrice());
			item.setSellerId(sellerId);

			Long newId = itemDAO.createItem(request);
			if (newId != null) {
				request.setItemId(newId);
				return new ItemResponse(true, SUCCESS_CREATE, request);
			}
		} catch (IllegalArgumentException e) {
			return new ItemResponse(false, e.getMessage(), null);
		} catch (Exception e) {
			String msg = e.getMessage();
			if ("DATABASE_ERROR".equals(msg)) {
				return new ItemResponse(false, msg, null);
			}
			return new ItemResponse(false, ERROR_SAVE_FAILED, null);
		}
		return new ItemResponse(false, ERROR_SAVE_FAILED, null);
	}

	@Override
	public ItemResponse updateItem(Long itemId, ItemRequest request, Long sellerId) {
		String validationError = validateRequest(request);
		if (validationError != null) {
			return new ItemResponse(false, validationError, null);
		}

		try {
			Item existing = itemDAO.findItem(itemId);

			if (!sellerId.equals(existing.getSellerId())) {
				return new ItemResponse(false, ERROR_NOT_OWNER, null);
			}

			String type = request.getType().trim().toUpperCase();
			ItemFactory itemFactory = ItemServiceImpl.factoryRegistry.get(type);

			if (itemFactory == null) {
				throw new IllegalArgumentException(ERROR_INVALID_ITEM_TYPE + " " + type);
			}
			Item item = itemFactory.createItem();

			item.setId(itemId);
			item.setName(request.getName());
			item.setDescription(request.getDescription());
			item.setStartingPrice(request.getStartingPrice());
			item.setCurrentPrice(request.getStartingPrice());
			item.setSellerId(sellerId);


			boolean result = itemDAO.updateItem(request);
			if (result) {
				return new ItemResponse(true, SUCCESS_UPDATE, request);
			}
		} catch (IllegalArgumentException e) {
			return new ItemResponse(false, e.getMessage(), null);
		} catch (Exception e) {
			String msg = e.getMessage();
			if ("ITEM_NOT_FOUND".equals(msg)) {
				return new ItemResponse(false, ERROR_ITEM_NOT_FOUND, null);
			}
			if ("DATABASE_ERROR".equals(msg)) {
				return new ItemResponse(false, msg, null);
			}
			return new ItemResponse(false, ERROR_SAVE_FAILED, null);
		}
		return new ItemResponse(false, ERROR_SAVE_FAILED, null);
	}

	@Override
	public ItemResponse deleteItem(Long itemId, Long sellerId) {
		try {
			Item existing = itemDAO.findItem(itemId);

			if (!sellerId.equals(existing.getSellerId())) {
				return new ItemResponse(false, ERROR_NOT_OWNER, null);
			}

			boolean result = itemDAO.deleteItem(itemId);
			if (result) {
				return new ItemResponse(true, SUCCESS_DELETE, null);
			}
		} catch (Exception e) {
			String msg = e.getMessage();
			if ("ITEM_NOT_FOUND".equals(msg)) {
				return new ItemResponse(false, ERROR_ITEM_NOT_FOUND, null);
			}
			if ("DATABASE_ERROR".equals(msg)) {
				return new ItemResponse(false, msg, null);
			}
			return new ItemResponse(false, ERROR_DELETE_FAILED, null);
		}
		return new ItemResponse(false, ERROR_DELETE_FAILED, null);
	}

	@Override
	public List<Item> findByType(String type) {
		if (type == null || type.isBlank()) {
			return List.of();
		}
		return itemDAO.findByType(type.toUpperCase());
	}

	@Override
	public List<Item> findBySeller(Long sellerId) {
		if (sellerId == null || sellerId <= 0) {
			return List.of();
		}
		return itemDAO.findBySeller(sellerId);
	}

	public String validateRequest(ItemRequest req) {
		if (req == null)
			return ERROR_INVALID_REQUEST;
		if (req.getName() == null || req.getName().isBlank())
			return ERROR_NAME_REQUIRED;
		if (req.getName().length() > 200)
			return ERROR_NAME_TOO_LONG;
		return null;
	}
}