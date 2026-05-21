package com.auction.service.impl;

import com.auction.dao.AuctionDAO; // Giả sử bạn đã tạo class này
import com.auction.dao.BidDAO;
import com.auction.dto.BidRequest;
import com.auction.dto.BidResponse;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.BidTransaction;
import com.auction.service.AuctionService;
import com.auction.service.BidService;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

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
	private static final double MIN_INCREMENT = 1.0;

	// SỬA: Thay Repository bằng DAO
	private final BidDAO bidDAO;
	private final AuctionDAO auctionDAO;
	private final AuctionService auctionService;

	// Quản lý khóa chống đụng độ (Concurrent Bidding)
	private final ConcurrentHashMap<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

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

		Long auctionId = request.getAuctionId();
		ReentrantLock lock = auctionLocks.computeIfAbsent(auctionId, k -> new ReentrantLock());

		// CHẶN CỬA: Chỉ cho phép 1 luồng xử lý bid cho 1 auction tại 1 thời điểm
		lock.lock();
		try {
			return processBid(request, bidderId);
		} finally {
			lock.unlock(); // Đảm bảo luôn mở khóa dù có lỗi xảy ra
		}
	}

	private BidResponse processBid(BidRequest request, Long bidderId) {
		Long auctionId = request.getAuctionId();
		double amount = request.getAmount();

		auctionService.refreshStatus(auctionId);

		try {
			// DAO ném lỗi nếu không tìm thấy, nên ta bắt ở khối catch
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

			// 1. Lưu giao dịch đấu giá
			BidTransaction bid = new BidTransaction();
			bid.setAuctionId(auctionId);
			bid.setBidderId(bidderId);
			bid.setAmount(amount);
			bidDAO.saveBid(bid);

			// 2. Cập nhật giá trị và người thắng hiện tại cho Auction
			auction.setCurrentPrice(amount);
			auction.setWinnerId(bidderId);
			auctionDAO.updateAuction(auction);

			return new BidResponse(true, SUCCESS_BID, bid, amount);

		} catch (Exception e) {
			String msg = e.getMessage();
			double currentPrice = 0; // Hoặc bạn có thể truy vấn lại giá nếu cần

			if ("AUCTION_NOT_FOUND".equals(msg)) {
				return new BidResponse(false, ERROR_AUCTION_NOT_FOUND, null, 0);
			}
			if ("DATABASE_ERROR".equals(msg)) {
				return new BidResponse(false, msg, null, 0);
			}
			return new BidResponse(false, ERROR_SAVE_FAILED, null, 0);
		}
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
