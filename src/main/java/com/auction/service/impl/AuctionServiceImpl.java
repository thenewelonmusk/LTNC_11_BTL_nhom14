package com.auction.service.impl;

import com.auction.dao.AuctionDAO;
import com.auction.dao.ItemDAO;
import com.auction.dto.AuctionRequest;
import com.auction.dto.AuctionResponse;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.item.Item;
import com.auction.service.AuctionService;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionServiceImpl implements AuctionService {
	private static final String ERROR_INVALID_REQUEST = "Yêu cầu không hợp lệ.";
	private static final String ERROR_ITEM_NOT_FOUND = "Không tìm thấy sản phẩm.";
	private static final String ERROR_NOT_OWNER = "Bạn không phải chủ sản phẩm này.";
	private static final String ERROR_INVALID_PRICE = "Giá khởi điểm phải > 0.";
	private static final String ERROR_INVALID_TIME = "Thời gian không hợp lệ.";
	private static final String ERROR_TIME_IN_PAST = "Thời gian bắt đầu không thể ở quá khứ.";
	private static final String ERROR_AUCTION_NOT_FOUND = "Không tìm thấy phiên đấu giá.";
	private static final String ERROR_CANNOT_CANCEL = "Không thể hủy phiên đã kết thúc.";
	private static final String ERROR_CANNOT_FINISH = "Không thể kết thúc phiên đã đóng.";
	private static final String ERROR_STILL_RUNNING = "Phiên đấu giá chưa kết thúc.";
	private static final String ERROR_SAVE_FAILED = "Lỗi hệ thống, không thể lưu.";
	private static final String ERROR_ITEM_ALREADY_AUCTIONED = "Sản phẩm đã có phiên đấu giá đang mở.";
	private static final String SUCCESS_OPEN = "Mở phiên đấu giá thành công.";
	private static final String SUCCESS_CANCEL = "Hủy phiên đấu giá thành công.";
	private static final String SUCCESS_FINISH = "Kết thúc phiên đấu giá thành công.";
	private static final String SUCCESS_FOUND = "OK.";

	private final AuctionDAO auctionDAO;
	private final ItemDAO itemDAO;

	public AuctionServiceImpl(AuctionDAO auctionDAO, ItemDAO itemDAO) {
		this.auctionDAO = auctionDAO;
		this.itemDAO = itemDAO;
	}

	@Override
	public AuctionResponse openAuction(AuctionRequest req, Long sellerId) {
		String err = validateRequest(req);
		if (err != null)
			return new AuctionResponse(false, err, null);

		try {
			Item item = itemDAO.findItem(req.getItemId());
			if (!sellerId.equals(item.getSellerId())) {
				return new AuctionResponse(false, ERROR_NOT_OWNER, null);
			}

			List<Auction> list = auctionDAO.findByItemId(req.getItemId());
			for (Auction a : list) {
				if (a.getStatus() == AuctionStatus.OPEN || a.getStatus() == AuctionStatus.RUNNING) {
					return new AuctionResponse(false, ERROR_ITEM_ALREADY_AUCTIONED, null);
				}
			}

			Auction a = new Auction();
			a.setItemId(req.getItemId());
			a.setSellerId(sellerId);
			a.setStartingPrice(req.getStartingPrice());
			a.setCurrentPrice(req.getStartingPrice());
			a.setStartTime(req.getStartTime());
			a.setEndTime(req.getEndTime());

			LocalDateTime now = LocalDateTime.now();
			if (now.isBefore(req.getStartTime())) {
				a.setStatus(AuctionStatus.OPEN);
			} else {
				a.setStatus(AuctionStatus.RUNNING);
			}

			if (auctionDAO.createAuction(a)) {
				return new AuctionResponse(true, SUCCESS_OPEN, a);
			}
		} catch (Exception e) {
			String msg = e.getMessage();
			if ("ITEM_NOT_FOUND".equals(msg))
				return new AuctionResponse(false, ERROR_ITEM_NOT_FOUND, null);
			if ("DATABASE_ERROR".equals(msg))
				return new AuctionResponse(false, msg, null);
		}
		return new AuctionResponse(false, ERROR_SAVE_FAILED, null);
	}

	@Override
	public AuctionResponse cancelAuction(Long auctionId, Long sellerId) {
		try {
			Auction a = auctionDAO.findAuction(auctionId);
			if (!sellerId.equals(a.getSellerId())) {
				return new AuctionResponse(false, ERROR_NOT_OWNER, null);
			}

			if (a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.PAID
					|| a.getStatus() == AuctionStatus.CANCELED) {
				return new AuctionResponse(false, ERROR_CANNOT_CANCEL, null);
			}

			a.setStatus(AuctionStatus.CANCELED);
			if (auctionDAO.updateAuction(a)) {
				return new AuctionResponse(true, SUCCESS_CANCEL, a);
			}
		} catch (Exception e) {
			String msg = e.getMessage();
			if ("AUCTION_NOT_FOUND".equals(msg))
				return new AuctionResponse(false, ERROR_AUCTION_NOT_FOUND, null);
			if ("DATABASE_ERROR".equals(msg))
				return new AuctionResponse(false, msg, null);
		}
		return new AuctionResponse(false, ERROR_SAVE_FAILED, null);
	}

	@Override
	public AuctionResponse finishAuction(Long auctionId) {
		try {
			Auction a = auctionDAO.findAuction(auctionId);
			if (LocalDateTime.now().isBefore(a.getEndTime())) {
				return new AuctionResponse(false, ERROR_STILL_RUNNING, null);
			}

			if (a.getStatus() == AuctionStatus.FINISHED || a.getStatus() == AuctionStatus.PAID
					|| a.getStatus() == AuctionStatus.CANCELED) {
				return new AuctionResponse(false, ERROR_CANNOT_FINISH, null);
			}

			a.setStatus(AuctionStatus.FINISHED);
			if (auctionDAO.updateAuction(a)) {
				return new AuctionResponse(true, SUCCESS_FINISH, a);
			}
		} catch (Exception e) {
			String msg = e.getMessage();
			if ("AUCTION_NOT_FOUND".equals(msg))
				return new AuctionResponse(false, ERROR_AUCTION_NOT_FOUND, null);
			if ("DATABASE_ERROR".equals(msg))
				return new AuctionResponse(false, msg, null);
		}
		return new AuctionResponse(false, ERROR_SAVE_FAILED, null);
	}

	@Override
	public void refreshStatus(Long auctionId) {
		try {
			Auction a = auctionDAO.findAuction(auctionId);
			LocalDateTime now = LocalDateTime.now();
			AuctionStatus cur = a.getStatus();

			if (cur == AuctionStatus.OPEN && !now.isBefore(a.getStartTime())) {
				a.setStatus(AuctionStatus.RUNNING);
				auctionDAO.updateAuction(a);
			} else if (cur == AuctionStatus.RUNNING && now.isAfter(a.getEndTime())) {
				a.setStatus(AuctionStatus.FINISHED);
				auctionDAO.updateAuction(a);
			}
		} catch (Exception e) {
		}
	}

	@Override
	public AuctionResponse findAuctionById(Long auctionId) {
		try {
			Auction a = auctionDAO.findAuction(auctionId);
			return new AuctionResponse(true, SUCCESS_FOUND, a);
		} catch (Exception e) {
			String msg = e.getMessage();
			if ("AUCTION_NOT_FOUND".equals(msg))
				return new AuctionResponse(false, ERROR_AUCTION_NOT_FOUND, null);
			if ("DATABASE_ERROR".equals(msg))
				return new AuctionResponse(false, msg, null);
		}
		return new AuctionResponse(false, ERROR_AUCTION_NOT_FOUND, null);
	}

	@Override
	public List<Auction> getAllAuctions() {
		return auctionDAO.findAll();
	}

	@Override
	public List<Auction> getAuctionsByStatus(AuctionStatus status) {
		return auctionDAO.findByStatus(status);
	}

	@Override
	public List<Auction> getAuctionsBySeller(Long sellerId) {
		return auctionDAO.findBySellerId(sellerId);
	}

	public String validateRequest(AuctionRequest req) {
		if (req == null)
			return ERROR_INVALID_REQUEST;
		if (req.getItemId() == null)
			return ERROR_INVALID_REQUEST;
		if (req.getStartingPrice() <= 0)
			return ERROR_INVALID_PRICE;
		if (req.getStartTime() == null || req.getEndTime() == null)
			return ERROR_INVALID_TIME;
		if (!req.getEndTime().isAfter(req.getStartTime()))
			return ERROR_INVALID_TIME;
		if (req.getStartTime().isBefore(LocalDateTime.now().minusMinutes(1)))
			return ERROR_TIME_IN_PAST;
		return null;
	}
}
