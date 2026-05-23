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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Triển khai chức năng Auto-Bidding.
 *
 * Lưu trữ in-memory (đề bài không yêu cầu persist auto-bid qua DB).
 * Thread-safety:
 *   - Map registry dùng ConcurrentHashMap.
 *   - Vòng đấu auto-bid trên một phiên được đồng bộ qua cùng cơ chế khóa
 *     của BidServiceImpl: AutoBidServiceImpl được TRUYỀN VÀO BidServiceImpl,
 *     và BidServiceImpl gọi triggerAutoBids() từ trong khối lock của nó.
 *     Nhờ vậy, vòng đấu auto-bid và bid thường không bao giờ chạy chồng nhau
 *     trên cùng một auctionId -> tránh lost update, double-winner, rollback giá.
 */
public class AutoBidServiceImpl implements AutoBidService {

    // ===== Messages =====
    private static final String ERR_INVALID_REQUEST   = "Yêu cầu không hợp lệ.";
    private static final String ERR_AUCTION_NOT_FOUND = "Không tìm thấy phiên đấu giá.";
    private static final String ERR_AUCTION_NOT_OPEN  = "Phiên đấu giá đã kết thúc hoặc bị hủy.";
    private static final String ERR_SELLER_CANNOT    = "Chủ sản phẩm không thể đăng ký auto-bid.";
    private static final String ERR_MAX_TOO_LOW      = "maxBid phải cao hơn giá hiện tại.";
    private static final String ERR_INCREMENT_INVALID= "increment phải > 0.";
    private static final String ERR_NOT_FOUND        = "Không tìm thấy auto-bid để hủy.";

    private static final String OK_REGISTERED = "Đăng ký auto-bid thành công.";
    private static final String OK_REPLACED   = "Đã cập nhật auto-bid cho phiên này.";
    private static final String OK_CANCELED   = "Đã hủy auto-bid.";

    private static final double MIN_INCREMENT = 1.0;

    // ===== Dependencies =====
    private final BidDAO bidDAO;
    private final AuctionDAO auctionDAO;

    // ===== Storage =====
    // auctionId -> (bidderId -> entry)
    private final ConcurrentHashMap<Long, Map<Long, AutoBidEntry>> registry = new ConcurrentHashMap<>();

    // id generator cho AutoBidEntry (in-memory)
    private final AtomicLong idSeq = new AtomicLong(1);

    public AutoBidServiceImpl(BidDAO bidDAO, AuctionDAO auctionDAO) {
        this.bidDAO = bidDAO;
        this.auctionDAO = auctionDAO;
    }

    // =========================================================
    // 1. ĐĂNG KÝ
    // =========================================================
    @Override
    public AutoBidResponse registerAutoBid(AutoBidRequest req) {
        String err = validate(req);
        if (err != null) {
            return new AutoBidResponse(false, err, null);
        }

        try {
            Auction auction = auctionDAO.findAuction(req.getAuctionId());

            if (auction.getStatus() == AuctionStatus.FINISHED
                    || auction.getStatus() == AuctionStatus.PAID
                    || auction.getStatus() == AuctionStatus.CANCELED) {
                return new AutoBidResponse(false, ERR_AUCTION_NOT_OPEN, null);
            }

            if (req.getBidderId().equals(auction.getSellerId())) {
                return new AutoBidResponse(false, ERR_SELLER_CANNOT, null);
            }

            if (req.getMaxBid() <= auction.getCurrentPrice()) {
                return new AutoBidResponse(false, ERR_MAX_TOO_LOW, null);
            }

            Map<Long, AutoBidEntry> perAuction =
                    registry.computeIfAbsent(req.getAuctionId(), k -> new ConcurrentHashMap<>());

            AutoBidEntry existing = perAuction.get(req.getBidderId());
            AutoBidEntry entry;
            String msg;

            if (existing != null) {
                // Giữ nguyên registeredAt cũ -> không mất quyền ưu tiên.
                existing.setMaxBid(req.getMaxBid());
                existing.setIncrement(req.getIncrement());
                existing.setActive(true);
                entry = existing;
                msg = OK_REPLACED;
            } else {
                entry = new AutoBidEntry(req.getAuctionId(), req.getBidderId(),
                        req.getMaxBid(), req.getIncrement());
                entry.setId(idSeq.getAndIncrement());
                perAuction.put(req.getBidderId(), entry);
                msg = OK_REGISTERED;
            }

            // LƯU Ý: KHÔNG gọi triggerAutoBids() ở đây trực tiếp để tránh
            // đệ quy / deadlock với BidServiceImpl. Người gọi nên đăng ký xong
            // rồi gọi BidService.placeBid() bình thường (hoặc gọi
            // triggerAutoBids từ controller sau khi đăng ký).
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
    /**
     * Vòng đấu giữa các auto-bid của cùng một phiên.
     *
     * Thuật toán:
     *   - Auto-bid chỉ phản ứng khi đã CÓ AI đó giữ giá (winnerId != null).
     *     Nếu chưa ai bid, vòng đấu không tự khởi động (đúng nghĩa "auto-bid là
     *     tự động trả thay" - phải có bid của đối thủ trước).
     *   - Lặp cho đến khi không còn ai có thể vượt giá hiện tại:
     *     1. Lọc các auto-bid ACTIVE, KHÔNG phải người đang giữ giá cao nhất,
     *        và còn khả năng trả (maxBid >= currentPrice + max(MIN_INCREMENT, increment riêng)).
     *     2. Nếu không còn ai -> thoát.
     *     3. Trong các ứng viên, chọn người có maxBid CAO NHẤT.
     *        Nếu nhiều người cùng maxBid cao nhất: chọn người đăng ký SỚM NHẤT.
     *     4. Tính nextPrice = currentPrice + increment của người đó.
     *        Nếu nextPrice vượt maxBid của họ -> ép xuống maxBid.
     *     5. Lưu BidTransaction (autoBid=true), cập nhật auction.
     *     6. Tiếp tục vòng lặp.
     *
     * Giới hạn an toàn: tối đa MAX_ROUNDS lần lặp để tránh vô hạn.
     */
    @Override
    public int triggerAutoBids(Long auctionId) {
        if (auctionId == null) return 0;

        Map<Long, AutoBidEntry> perAuction = registry.get(auctionId);
        if (perAuction == null || perAuction.isEmpty()) return 0;

        final int MAX_ROUNDS = 1000;
        int placed = 0;

        try {
            Auction auction = auctionDAO.findAuction(auctionId);

            if (auction.getStatus() != AuctionStatus.RUNNING) {
                return 0;
            }

            // Auto-bid chỉ phản ứng khi đã có người giữ giá. Không tự khởi động
            // khi chưa ai bid - phải có một bid thật (hoặc auto-bid khác) trước.
            if (auction.getWinnerId() == null) {
                return 0;
            }

            for (int round = 0; round < MAX_ROUNDS; round++) {
                double currentPrice = auction.getCurrentPrice();
                Long currentWinner = auction.getWinnerId();

                // Bước 1+3: chọn ứng viên tốt nhất.
                AutoBidEntry chosen = selectNextAutoBid(perAuction, currentPrice, currentWinner);
                if (chosen == null) {
                    break; // hết người có thể vượt giá -> dừng.
                }

                // Bước 4: tính nextPrice
                double step = Math.max(MIN_INCREMENT, chosen.getIncrement());
                double nextPrice = currentPrice + step;
                if (nextPrice > chosen.getMaxBid()) {
                    nextPrice = chosen.getMaxBid();
                }
                // An toàn: nextPrice phải thực sự vượt giá hiện tại tối thiểu MIN_INCREMENT.
                if (nextPrice < currentPrice + MIN_INCREMENT) {
                    // Người này không thể đặt giá hợp lệ -> tắt auto-bid của họ.
                    chosen.setActive(false);
                    continue;
                }

                // Bước 5: lưu bid và cập nhật auction
                BidTransaction bid = new BidTransaction();
                bid.setAuctionId(auctionId);
                bid.setBidderId(chosen.getBidderId());
                bid.setAmount(nextPrice);
                bid.setAutoBid(true);

                try {
                    bidDAO.saveBid(bid);
                } catch (Exception e) {
                    // Lỗi DB -> dừng vòng đấu auto-bid, không làm sập hệ thống.
                    break;
                }

                auction.setCurrentPrice(nextPrice);
                auction.setWinnerId(chosen.getBidderId());

                try {
                    auctionDAO.updateAuction(auction);
                } catch (Exception e) {
                    break;
                }

                placed++;

                // Nếu người này đã chạm maxBid -> đánh dấu inactive, không cần xét nữa.
                if (nextPrice >= chosen.getMaxBid()) {
                    chosen.setActive(false);
                }
            }
        } catch (Exception e) {
            // AUCTION_NOT_FOUND hoặc DB error -> bỏ qua, trả về số bid đã đặt được.
        }

        return placed;
    }

    /**
     * Chọn auto-bid tốt nhất để đặt tiếp theo:
     *   - active = true
     *   - bidder != currentWinner (không tự đè giá của chính mình)
     *   - maxBid >= currentPrice + MIN_INCREMENT  (còn khả năng vượt)
     *   - sắp xếp: maxBid giảm dần, registeredAt tăng dần (ưu tiên đăng ký sớm)
     */
    private AutoBidEntry selectNextAutoBid(Map<Long, AutoBidEntry> perAuction,
                                           double currentPrice, Long currentWinner) {
        List<AutoBidEntry> candidates = new ArrayList<>();
        for (AutoBidEntry e : perAuction.values()) {
            if (!e.isActive()) continue;
            if (e.getBidderId().equals(currentWinner)) continue; // không tự bid chính mình
            if (e.getMaxBid() < currentPrice + MIN_INCREMENT) continue; // không đủ tiền
            candidates.add(e);
        }
        if (candidates.isEmpty()) return null;

        candidates.sort((a, b) -> {
            int cmp = Double.compare(b.getMaxBid(), a.getMaxBid()); // maxBid giảm dần
            if (cmp != 0) return cmp;
            return a.getRegisteredAt().compareTo(b.getRegisteredAt()); // sớm hơn ưu tiên
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
