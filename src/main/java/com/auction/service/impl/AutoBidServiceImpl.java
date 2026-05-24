package com.auction.service.impl;

import com.auction.dao.AuctionDAO;
import com.auction.dao.BidDAO;
import com.auction.dto.AutoBidRequest;
import com.auction.dto.AutoBidResponse;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.AutoBidEntry;
import com.auction.model.BidTransaction;
import com.auction.service.AutoBidService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Triển khai Auto-Bidding.
 *
 * Lưu trữ in-memory; thread-safety đạt được bằng cách dùng CHUNG bảng lock
 * per-auction với BidServiceImpl. Mọi thao tác đặt giá (manual hay auto)
 * đều phải nắm lock đó trước khi sửa Auction -> tránh lost update.
 */
public class AutoBidServiceImpl implements AutoBidService {

	// ===== Messages =====
	private static final String ERR_INVALID_REQUEST = "Yêu cầu không hợp lệ.";
	private static final String ERR_AUCTION_NOT_FOUND = "Không tìm thấy phiên đấu giá.";
	private static final String ERR_AUCTION_NOT_OPEN = "Phiên đấu giá đã kết thúc hoặc bị hủy.";
	private static final String ERR_SELLER_CANNOT = "Chủ sản phẩm không thể đăng ký auto-bid.";
	private static final String ERR_MAX_TOO_LOW = "maxBid phải cao hơn giá hiện tại.";
	private static final String ERR_INCREMENT_INVALID = "increment phải > 0.";
	private static final String ERR_NOT_FOUND = "Không tìm thấy auto-bid để hủy.";

	private static final String OK_REGISTERED = "Đăng ký auto-bid thành công.";
	private static final String OK_REPLACED = "Đã cập nhật auto-bid cho phiên này.";
	private static final String OK_CANCELED = "Đã hủy auto-bid.";

	private static final double MIN_INCREMENT = 1.0;
	private static final long ANTI_SNIPING_TRIGGER_SECONDS = 30L;
	private static final long ANTI_SNIPING_EXTEND_SECONDS = 60L;

	private final BidDAO bidDAO;
	private final AuctionDAO auctionDAO;

	// auctionId -> (bidderId -> entry)
	private final ConcurrentHashMap<Long, Map<Long, AutoBidEntry>> registry = new ConcurrentHashMap<>();
	private final AtomicLong idSeq = new AtomicLong(1);

	// Lock map dùng chung với BidServiceImpl
	private ConcurrentHashMap<Long, ReentrantLock> sharedLocks;
	private final ConcurrentHashMap<Long, ReentrantLock> ownLocks = new ConcurrentHashMap<>();

	public AutoBidServiceImpl(BidDAO bidDAO, AuctionDAO auctionDAO) {
		this.bidDAO = bidDAO;
		this.auctionDAO = auctionDAO;
	}

	/** Được BidServiceImpl gọi để chia sẻ chung lock map. */
	public void setSharedLockMap(ConcurrentHashMap<Long, ReentrantLock> shared) {
		this.sharedLocks = shared;
	}

	private ReentrantLock lockOf(Long auctionId) {
		ConcurrentHashMap<Long, ReentrantLock> map = sharedLocks != null ? sharedLocks : ownLocks;
		return map.computeIfAbsent(auctionId, k -> new ReentrantLock());
	}

	// =========================================================
	// 1. ĐĂNG KÝ (không kích hoạt vòng đấu)
	// =========================================================
	@Override
	public AutoBidResponse registerAutoBid(AutoBidRequest req) {
		String err = validate(req);
		if (err != null) {
			return new AutoBidResponse(false, err, null);
		}

		ReentrantLock lock = lockOf(req.getAuctionId());
		lock.lock();
		try {
			return doRegister(req);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Đăng ký + kích hoạt vòng đấu trong CÙNG lock. Mỗi auto-bid đặt được sẽ gọi
	 * listener để server broadcast realtime.
	 */
	@Override
	public AutoBidResponse registerAutoBidAndTrigger(AutoBidRequest req, AutoBidListener listener) {
		String err = validate(req);
		if (err != null) {
			return new AutoBidResponse(false, err, null);
		}

		ReentrantLock lock = lockOf(req.getAuctionId());
		lock.lock();
		try {
			AutoBidResponse res = doRegister(req);
			if (!res.isSuccess()) {
				return res;
			}

			// Sau khi đăng ký, kích hoạt vòng đấu auto-bid ngay (cùng lock).
			triggerAutoBidsInternal(req.getAuctionId(), listener);
			return res;
		} finally {
			lock.unlock();
		}
	}

	private AutoBidResponse doRegister(AutoBidRequest req) {
		try {
			Auction auction = auctionDAO.findAuction(req.getAuctionId());

			if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.PAID
					|| auction.getStatus() == AuctionStatus.CANCELED) {
				return new AutoBidResponse(false, ERR_AUCTION_NOT_OPEN, null);
			}
			if (req.getBidderId().equals(auction.getSellerId())) {
				return new AutoBidResponse(false, ERR_SELLER_CANNOT, null);
			}
			if (req.getMaxBid() <= auction.getCurrentPrice()) {
				return new AutoBidResponse(false, ERR_MAX_TOO_LOW, null);
			}

			Map<Long, AutoBidEntry> perAuction = registry.computeIfAbsent(req.getAuctionId(),
					k -> new ConcurrentHashMap<>());

			AutoBidEntry existing = perAuction.get(req.getBidderId());
			AutoBidEntry entry;
			String msg;

			if (existing != null) {
				existing.setMaxBid(req.getMaxBid());
				existing.setIncrement(req.getIncrement());
				existing.setActive(true);
				entry = existing;
				msg = OK_REPLACED;
			} else {
				entry = new AutoBidEntry(req.getAuctionId(), req.getBidderId(), req.getMaxBid(), req.getIncrement());
				entry.setId(idSeq.getAndIncrement());
				perAuction.put(req.getBidderId(), entry);
				msg = OK_REGISTERED;
			}
			return new AutoBidResponse(true, msg, entry);

		} catch (Exception e) {
			String m = e.getMessage();
			if ("AUCTION_NOT_FOUND".equals(m)) {
				return new AutoBidResponse(false, ERR_AUCTION_NOT_FOUND, null);
			}
			return new AutoBidResponse(false, ERR_INVALID_REQUEST, null);
		}
	}

	// =========================================================
	// 2. HỦY
	// =========================================================
	@Override
	public AutoBidResponse cancelAutoBid(Long auctionId, Long bidderId) {
		if (auctionId == null || bidderId == null) {
			return new AutoBidResponse(false, ERR_INVALID_REQUEST, null);
		}
		Map<Long, AutoBidEntry> perAuction = registry.get(auctionId);
		if (perAuction == null) {
			return new AutoBidResponse(false, ERR_NOT_FOUND, null);
		}
		AutoBidEntry removed = perAuction.remove(bidderId);
		if (removed == null) {
			return new AutoBidResponse(false, ERR_NOT_FOUND, null);
		}
		removed.setActive(false);
		return new AutoBidResponse(true, OK_CANCELED, removed);
	}

	@Override
	public AutoBidEntry getAutoBid(Long auctionId, Long bidderId) {
		Map<Long, AutoBidEntry> perAuction = registry.get(auctionId);
		return perAuction == null ? null : perAuction.get(bidderId);
	}

	@Override
	public List<AutoBidEntry> getAutoBidsByBidder(Long bidderId) {
		List<AutoBidEntry> result = new ArrayList<>();
		for (Map<Long, AutoBidEntry> perAuction : registry.values()) {
			AutoBidEntry e = perAuction.get(bidderId);
			if (e != null && e.isActive()) {
				result.add(e);
			}
		}
		return result;
	}

	// =========================================================
	// 3. KÍCH HOẠT VÒNG ĐẤU AUTO-BID
	// =========================================================
	@Override
	public int triggerAutoBids(Long auctionId) {
		return triggerAutoBids(auctionId, null);
	}

	@Override
	public int triggerAutoBids(Long auctionId, AutoBidListener listener) {
		if (auctionId == null) return 0;
		ReentrantLock lock = lockOf(auctionId);
		// Nếu caller (BidServiceImpl) đã nắm lock thì lock.lock() ở đây là reentrant -> OK.
		lock.lock();
		try {
			return triggerAutoBidsInternal(auctionId, listener);
		} finally {
			lock.unlock();
		}
	}

	/**
	 * Phải được gọi trong khi đã nắm lockOf(auctionId).
	 */
	private int triggerAutoBidsInternal(Long auctionId, AutoBidListener listener) {
		Map<Long, AutoBidEntry> perAuction = registry.get(auctionId);
		if (perAuction == null || perAuction.isEmpty()) return 0;

		final int MAX_ROUNDS = 1000;
		int placed = 0;

		try {
			Auction auction = auctionDAO.findAuction(auctionId);
			if (auction.getStatus() != AuctionStatus.RUNNING) {
				return 0;
			}

			for (int round = 0; round < MAX_ROUNDS; round++) {
				double currentPrice = auction.getCurrentPrice();
				Long currentWinner = auction.getWinnerId();

				AutoBidEntry chosen = selectNextAutoBid(perAuction, currentPrice, currentWinner);
				if (chosen == null) break;

				double step = Math.max(MIN_INCREMENT, chosen.getIncrement());
				double nextPrice = currentPrice + step;

				if (nextPrice > chosen.getMaxBid()) {
					nextPrice = chosen.getMaxBid();
				}
				if (nextPrice < currentPrice + MIN_INCREMENT) {
					chosen.setActive(false);
					continue;
				}

				BidTransaction bid = new BidTransaction();
				bid.setAuctionId(auctionId);
				bid.setBidderId(chosen.getBidderId());
				bid.setAmount(nextPrice);
				bid.setAutoBid(true);

// 🌟 BỔ SUNG KHỐI TRANSACTION NÀY
				try {
					com.auction.dao.DatabaseConnection.beginTransaction(); // Mở kết nối mới cho Bot

					bidDAO.saveBid(bid);

					auction.setCurrentPrice(nextPrice);
					auction.setWinnerId(chosen.getBidderId());

					// Logic Anti-sniping
					LocalDateTime now = LocalDateTime.now();
					if (auction.getEndTime() != null
							&& now.isAfter(auction.getEndTime().minusSeconds(ANTI_SNIPING_TRIGGER_SECONDS))) {
						LocalDateTime fromOld = auction.getEndTime().plusSeconds(ANTI_SNIPING_EXTEND_SECONDS);
						LocalDateTime fromNow = now.plusSeconds(ANTI_SNIPING_EXTEND_SECONDS);
						auction.setEndTime(fromOld.isAfter(fromNow) ? fromOld : fromNow);
					}

					auctionDAO.updateAuction(auction);

					com.auction.dao.DatabaseConnection.commit(); // Lưu thành công, đóng kết nối

				} catch (Exception e) {
					com.auction.dao.DatabaseConnection.cleanup(); // Rollback nếu có lỗi
					System.err.println("Lỗi lưu DB của Bot: " + e.getMessage());
					throw new RuntimeException("Failed to save auto-bid: " + e.getMessage(), e);
				}

				auction.setCurrentPrice(nextPrice);
				auction.setWinnerId(chosen.getBidderId());

				// Anti-sniping áp dụng chuẩn cho cả Auto-bid
				LocalDateTime now = LocalDateTime.now();
				if (auction.getEndTime() != null
						&& now.isAfter(auction.getEndTime().minusSeconds(ANTI_SNIPING_TRIGGER_SECONDS))) {
					LocalDateTime fromOld = auction.getEndTime().plusSeconds(ANTI_SNIPING_EXTEND_SECONDS);
					LocalDateTime fromNow = now.plusSeconds(ANTI_SNIPING_EXTEND_SECONDS);
					auction.setEndTime(fromOld.isAfter(fromNow) ? fromOld : fromNow);
					System.out.println("[Anti-Sniping][Auto-Bid] Phiên " + auctionId + " được gia hạn đến "
							+ auction.getEndTime());
				}

				try {
					auctionDAO.updateAuction(auction);
				} catch (Exception e) {
					throw new RuntimeException("Failed to update auction: " + e.getMessage(), e);
				}

				placed++;

				// 🌟 FIX QUAN TRỌNG NHẤT Ở ĐÂY: Truyền nextPrice thay vì số 0 cứng
				if (listener != null) {
					try {
						listener.onAutoBidPlaced(bid, nextPrice);
					} catch (Exception ignored) {
					}
				}

				if (nextPrice >= chosen.getMaxBid()) {
					chosen.setActive(false);
				}
			}
		} catch (Exception e) {
			System.err.println("[AutoBidService] Error in triggerAutoBidsInternal: " + e.getMessage());
			e.printStackTrace();
			throw new RuntimeException(e);
		}

		return placed;
	}

	private AutoBidEntry selectNextAutoBid(Map<Long, AutoBidEntry> perAuction, double currentPrice, Long currentWinner) {
		List<AutoBidEntry> candidates = new ArrayList<>();
		for (AutoBidEntry e : perAuction.values()) {
			if (!e.isActive()) continue;
			if (currentWinner != null && e.getBidderId().equals(currentWinner)) continue;
			if (e.getMaxBid() < currentPrice + MIN_INCREMENT) continue;
			candidates.add(e);
		}
		if (candidates.isEmpty()) return null;

		candidates.sort((a, b) -> {
			int cmp = Double.compare(b.getMaxBid(), a.getMaxBid());
			if (cmp != 0) return cmp;
			return a.getRegisteredAt().compareTo(b.getRegisteredAt());
		});
		return candidates.get(0);
	}

	// =========================================================
	// 4. DỌN DẸP
	// =========================================================
	@Override
	public void clearAutoBidsForAuction(Long auctionId) {
		if (auctionId == null) return;
		registry.remove(auctionId);
	}

	// =========================================================
	// Helpers
	// =========================================================
	private String validate(AutoBidRequest r) {
		if (r == null || r.getAuctionId() == null || r.getBidderId() == null) {
			return ERR_INVALID_REQUEST;
		}
		if (r.getIncrement() <= 0) {
			return ERR_INCREMENT_INVALID;
		}
		if (r.getMaxBid() <= 0) {
			return ERR_INVALID_REQUEST;
		}
		return null;
	}
}
