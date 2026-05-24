package com.auction.service;

import com.auction.dto.AutoBidRequest;
import com.auction.dto.AutoBidResponse;
import com.auction.dto.BidRequest;
import com.auction.dto.BidResponse;
import com.auction.model.BidTransaction;

import java.util.List;

public interface BidService {
	BidResponse placeBid(BidRequest request, Long bidderId);

	/**
	 * Place bid với listener nhận từng auto-bid được đặt sau đó (nếu có). Cho phép
	 * server broadcast realtime theo từng bước nhảy giá thay vì chỉ phát một thông
	 * điệp với giá cuối cùng.
	 */
	BidResponse placeBid(BidRequest request, Long bidderId, AutoBidService.AutoBidListener listener);

	/**
	 * Đăng ký auto-bid và kích hoạt vòng đấu NGAY (nằm cùng lock với placeBid) —
	 * tránh tranh chấp với bid thường. Listener nhận từng auto-bid được đặt.
	 */
	AutoBidResponse registerAutoBidWithLock(AutoBidRequest request, AutoBidService.AutoBidListener listener);

	List<BidTransaction> getBidsByAuction(Long auctionId);
	List<BidTransaction> getBidsByBidder(Long bidderId);
	BidTransaction getHighestBid(Long auctionId);
}
