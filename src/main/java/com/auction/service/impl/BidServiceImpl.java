package com.auction.service.impl;

import com.auction.dao.AuctionDAO;
import com.auction.dao.BidDAO;
import com.auction.dto.BidRequest;
import com.auction.dto.BidResponse;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.BidTransaction;
import com.auction.service.AuctionService;
import com.auction.service.BidService;

import java.time.LocalDateTime;
import java.util.List;

public class BidServiceImpl implements BidService {
	// Error messages
	private static final String ERROR_INVALID_REQUEST = "Yêu cầu không hợp lệ.";
	private static final String ERROR_AUCTION_NOT_FOUND = "Không tìm thấy phiên đấu giá.";
	private static final String ERROR_AUCTION_NOT_RUNNING = "Phiên đấu giá chưa diễn ra hoặc đã kết thúc.";
	private static final String ERROR_BID_TOO_LOW = "Giá đặt phải cao hơn giá hiện tại.";
	private static final String ERROR_AMOUNT_INVALID = "Số tiền không hợp lệ.";
	private static final String ERROR_SELLER_CANNOT_BID = "Chủ sản phẩm không thể đấu giá.";
	private static final String ERROR_SAVE_FAILED = "Lỗi hệ thống, không thể lưu.";

	private static final String SUCCESS_BID = "Đặt giá thành công.";
	private static final String SUCCESS_BID_EXTENDED = "Đặt giá thành công & Hệ thống đã tự động gia hạn phiên đấu giá!";
	private static final double MIN_INCREMENT = 1.0;
	private static final long ANTI_SNIPING_TRIGGER_SECONDS = 30L;
	private static final long ANTI_SNIPING_EXTEND_SECONDS = 60L;

	private final BidDAO bidDAO;
	private final AuctionDAO auctionDAO;
	private final AuctionService auctionService;

	public BidServiceImpl(BidDAO bidDAO, AuctionDAO auctionDAO, AuctionService auctionService) {
		this.bidDAO = bidDAO;
		this.auctionDAO = auctionDAO;
		this.auctionService = auctionService;
	}

	@Override
	public BidResponse placeBid(BidRequest request, Long bidderId) {
		String error = validateRequest(request, bidderId);
		if (error != null) {
			return new BidResponse(false, error, null, 0);
		}

		try {
			com.auction.dao.DatabaseConnection.beginTransaction();
			BidResponse response = processBid(request, bidderId);
			com.auction.dao.DatabaseConnection.commit();
			return response;

		} catch (Exception e) {
			com.auction.dao.DatabaseConnection.cleanup();

			String msg = e.getMessage();
			if ("AUCTION_NOT_FOUND".equals(msg)) {
				return new BidResponse(false, ERROR_AUCTION_NOT_FOUND, null, 0);
			}
			System.err.println("[BidService] Transaction error: " + e.getMessage());
			e.printStackTrace();
			return new BidResponse(false, ERROR_SAVE_FAILED, null, 0);
		}
	}

	/**
	 * Xử lý bid thật sự
	 */
	private BidResponse processBid(BidRequest request, Long bidderId) throws Exception {
		Long auctionId = request.getAuctionId();
		double amount = request.getAmount();

		auctionService.refreshStatus(auctionId);

		Auction auction = auctionDAO.findAuction(auctionId);

		if (auction.getStatus() != AuctionStatus.RUNNING) {
			return new BidResponse(false, ERROR_AUCTION_NOT_RUNNING, null, auction.getCurrentPrice());
		}
		if (bidderId.equals(auction.getSellerId())) {
			return new BidResponse(false, ERROR_SELLER_CANNOT_BID, null, auction.getCurrentPrice());
		}
		if (amount < auction.getCurrentPrice() + MIN_INCREMENT) {
			return new BidResponse(false, ERROR_BID_TOO_LOW, null, auction.getCurrentPrice());
		}

		// 1. Lưu giao dịch của người dùng
		BidTransaction bid = new BidTransaction();
		bid.setAuctionId(auctionId);
		bid.setBidderId(bidderId);
		bid.setAmount(amount);
		bidDAO.saveBid(bid);

		// 2. Cập nhật giá hiện tại
		auction.setCurrentPrice(amount);
		auction.setWinnerId(bidderId);

		// 3. Logic Anti-sniping
		boolean extended = false;
		LocalDateTime now = LocalDateTime.now();
		if (auction.getEndTime() != null
				&& now.isAfter(auction.getEndTime().minusSeconds(ANTI_SNIPING_TRIGGER_SECONDS))) {
			LocalDateTime fromOld = auction.getEndTime().plusSeconds(ANTI_SNIPING_EXTEND_SECONDS);
			LocalDateTime fromNow = now.plusSeconds(ANTI_SNIPING_EXTEND_SECONDS);
			auction.setEndTime(fromOld.isAfter(fromNow) ? fromOld : fromNow);
			extended = true;
			System.out.println(
					"[Anti-Sniping] Phiên " + auctionId + " được tự động gia hạn đến " + auction.getEndTime());
		}

		auctionDAO.updateAuction(auction);

		// Trả kết quả (Khoan hãy Commit!)
		return new BidResponse(true, extended ? SUCCESS_BID_EXTENDED : SUCCESS_BID, bid, amount);
	}

	@Override
	public List<BidTransaction> getBidsByAuction(Long auctionId) {
		return bidDAO.findByAuctionId(auctionId);
	}

	@Override
	public List<BidTransaction> getBidsByBidder(Long bidderId) {
		return bidDAO.findByBidderId(bidderId);
	}

	@Override
	public BidTransaction getHighestBid(Long auctionId) {
		return bidDAO.findHighestBidByAuction(auctionId);
	}

	private String validateRequest(BidRequest req, Long bidderId) {
		if (req == null || req.getAuctionId() == null || bidderId == null) {
			return ERROR_INVALID_REQUEST;
		}
		if (req.getAmount() <= 0) {
			return ERROR_AMOUNT_INVALID;
		}
		return null;
	}
}