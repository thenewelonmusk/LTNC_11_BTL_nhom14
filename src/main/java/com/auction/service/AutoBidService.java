package com.auction.service;

import com.auction.dto.AutoBidRequest;
import com.auction.dto.AutoBidResponse;
import com.auction.model.AutoBidEntry;

import java.util.List;

/**
 * Service cho chức năng Auto-Bidding (Đấu giá tự động).
 *
 * Người dùng đăng ký maxBid và increment. Khi có bid mới từ đối thủ,
 * hệ thống tự động đặt giá thay người dùng theo bước {@code increment},
 * miễn là không vượt quá {@code maxBid}.
 */
public interface AutoBidService {

    /**
     * Đăng ký một Auto-Bid cho phiên đấu giá. Nếu người dùng đã có auto-bid
     * trên cùng phiên này, đăng ký mới sẽ thay thế cái cũ (giữ lại
     * registeredAt cũ để không mất quyền ưu tiên).
     */
    AutoBidResponse registerAutoBid(AutoBidRequest request);

    /**
     * Hủy auto-bid của một bidder trên một phiên cụ thể.
     */
    AutoBidResponse cancelAutoBid(Long auctionId, Long bidderId);

    /**
     * Lấy auto-bid hiện hành của một bidder trên một phiên (nếu có).
     */
    AutoBidEntry getAutoBid(Long auctionId, Long bidderId);

    /**
     * Liệt kê toàn bộ auto-bid của một bidder trên mọi phiên.
     */
    List<AutoBidEntry> getAutoBidsByBidder(Long bidderId);

    /**
     * Kích hoạt vòng đấu giữa các auto-bid của một phiên. Được gọi sau khi
     * có một bid (thường hoặc auto) thành công.
     *
     * Trả về số lượng auto-bid mà hệ thống đã đặt giúp trong lần kích hoạt này.
     */
    int triggerAutoBids(Long auctionId);

    /**
     * Dọn dẹp toàn bộ auto-bid của một phiên đã kết thúc/hủy.
     */
    void clearAutoBidsForAuction(Long auctionId);
}
