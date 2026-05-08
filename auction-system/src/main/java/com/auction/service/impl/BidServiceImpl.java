package com.auction.service.impl;

import com.auction.dto.BidRequest;
import com.auction.dto.BidResponse;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.BidTransaction;
import com.auction.repository.AuctionRepository;
import com.auction.repository.BidRepository;
import com.auction.service.AuctionService;
import com.auction.service.BidService;

import java.time.LocalDateTime;
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

    // Success messages
    private static final String SUCCESS_BID = "Đặt giá thành công.";

    // Minimum increment (giá phải cao hơn ít nhất 1 đơn vị)
    private static final double MIN_INCREMENT = 1.0;

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;

    /**
     * Mỗi auction có 1 lock riêng. Dùng ConcurrentHashMap để
     * thread-safe khi nhiều thread cùng yêu cầu lock.
     * Đây là chìa khóa giải quyết concurrent bidding.
     */
    private final ConcurrentHashMap<Long, ReentrantLock> auctionLocks = new ConcurrentHashMap<>();

    public BidServiceImpl(BidRepository bidRepository, AuctionRepository auctionRepository,AuctionService auctionService) {
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
        this.auctionService = auctionService;
    }

    @Override
    public BidResponse placeBid(BidRequest request, Long bidderId) {
        String error = validateRequest(request, bidderId);
        if (error != null) {
            return new BidResponse(false,error,null,0);
        }

        Long auctionId = request.getAuctionId();

        ReentrantLock lock = auctionLocks.computeIfAbsent(
                auctionId,k -> new ReentrantLock());

        lock.lock();
        try {
            return processBid(request,bidderId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Xử lý bid thực sự — đã được lock bảo vệ.
     * Mọi đọc/ghi auction.currentPrice ở đây là atomic.
     */
    private BidResponse processBid(BidRequest request, Long bidderId) {
        Long auctionId = request.getAuctionId();
        double amount = request.getAmount();

        auctionService.refreshStatus(auctionId);

        Auction auction = auctionRepository.findById(auctionId);
        if (auction == null) {
            return new BidResponse(false,ERROR_AUCTION_NOT_FOUND,null,0);
        }

        if (auction.getStatus() != AuctionStatus.RUNNING) {
            return new BidResponse(false,ERROR_AUCTION_NOT_RUNNING,null,auction.getCurrentPrice());
        }

        if (bidderId.equals(auction.getSellerId())) {
            return new BidResponse(false,ERROR_SELLER_CANNOT_BID,null, auction.getCurrentPrice());
        }

        if (amount < auction.getCurrentPrice() + MIN_INCREMENT) {
            return new BidResponse(false,ERROR_BID_TOO_LOW,null, auction.getCurrentPrice());
        }

        BidTransaction bid = new BidTransaction();
        bid.setAuctionId(auctionId);
        bid.setBidderId(bidderId);
        bid.setAmount(amount);

        boolean bidOk = bidRepository.save(bid);
        if (!bidOk) {
            return new BidResponse(false,ERROR_SAVE_FAILED,null, auction.getCurrentPrice());
        }

        auction.setCurrentPrice(amount);
        auction.setWinnerId(bidderId);

        boolean auctionOk = auctionRepository.save(auction);
        if (!auctionOk) {
            return new BidResponse(false,ERROR_SAVE_FAILED,null, auction.getCurrentPrice());
        }

        return new BidResponse(true,SUCCESS_BID,bid,amount);
    }

    @Override
    public List<BidTransaction> getBidsByAuction(Long auctionId) {
        return bidRepository.findByAuctionId(auctionId);
    }

    @Override
    public List<BidTransaction> getBidsByBidder(Long bidderId) {
        return bidRepository.findByBidderId(bidderId);
    }

    @Override
    public BidTransaction getHighestBid(Long auctionId) {
        return bidRepository.findHighestBidByAuction(auctionId);
    }

    /**
     * Validate input cơ bản (không cần lock).
     * Trả null nếu hợp lệ, ngược lại trả message lỗi.
     */
    private String validateRequest(BidRequest req, Long bidderId) {
        if (req == null) {
            return ERROR_INVALID_REQUEST;
        }
        if (req.getAuctionId() == null) {
            return ERROR_INVALID_REQUEST;
        }
        if (req.getAmount() <= 0) {
            return ERROR_AMOUNT_INVALID;
        }
        if (bidderId == null) {
            return ERROR_INVALID_REQUEST;
        }
        return null;
    }
}
