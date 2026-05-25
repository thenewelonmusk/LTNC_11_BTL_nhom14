package com.auction.service.impl;

import com.auction.dao.AuctionDAO;
import com.auction.dao.BidDAO;
import com.auction.dto.BidRequest;
import com.auction.dto.BidResponse;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.BidTransaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BidServiceImplTest {

	@Mock
	private BidDAO bidDAO;
	@Mock
	private AuctionDAO auctionDAO;
	@Mock
	private AuctionServiceImpl auctionService;

	private BidServiceImpl bidService;

	@BeforeEach
	void setUp() {
		bidService = new BidServiceImpl(bidDAO, auctionDAO, auctionService);
	}

	// ================= PLACE BID TESTS =================

	@Test
	void testPlaceBid_NoAntiSniping_WhenMoreThan30SecondsRemain() throws Exception {

		BidRequest req = new BidRequest();
		req.setAuctionId(10L);
		req.setAmount(200.0);

		Auction auction = new Auction();
		auction.setId(10L);
		auction.setSellerId(1L);
		auction.setStatus(AuctionStatus.RUNNING);
		auction.setCurrentPrice(100.0);

		// còn 5 phút -> KHÔNG được gia hạn
		LocalDateTime oldEndTime = LocalDateTime.now().plusMinutes(5);
		auction.setEndTime(oldEndTime);

		when(auctionDAO.findAuction(10L)).thenReturn(auction);

		when(bidDAO.saveBid(any(BidTransaction.class))).thenReturn(true);

		when(auctionDAO.updateAuction(auction)).thenReturn(true);

		BidResponse res = bidService.placeBid(req, 2L);

		assertTrue(res.isSuccess());

		assertEquals("Đặt giá thành công.", res.getMessage());

		// endTime giữ nguyên
		assertEquals(oldEndTime, auction.getEndTime());

		verify(auctionDAO, times(1)).updateAuction(auction);
	}

	@Test
	void testPlaceBid_SellerCannotBid() throws Exception {
		BidRequest req = new BidRequest();
		req.setAuctionId(10L);
		req.setAmount(150.0);

		Auction a = new Auction();
		a.setSellerId(1L);
		a.setStatus(AuctionStatus.RUNNING);

		when(auctionDAO.findAuction(10L)).thenReturn(a);

		// Chủ (ID=1) tự nhảy vào đấu giá sản phẩm của mình
		BidResponse res = bidService.placeBid(req, 1L);

		assertFalse(res.isSuccess());
		assertEquals("Chủ sản phẩm không thể đấu giá.", res.getMessage());
		verify(bidDAO, never()).saveBid(any());
	}

	@Test
	void testPlaceBid_BidTooLow() throws Exception {
		BidRequest req = new BidRequest();
		req.setAuctionId(10L);
		req.setAmount(100.5); // Tăng có 0.5 (Luật yêu cầu >= 1.0)

		Auction a = new Auction();
		a.setSellerId(1L);
		a.setStatus(AuctionStatus.RUNNING);
		a.setCurrentPrice(100.0);

		when(auctionDAO.findAuction(10L)).thenReturn(a);

		BidResponse res = bidService.placeBid(req, 2L);

		assertFalse(res.isSuccess());
		assertEquals("Giá đặt phải cao hơn giá hiện tại.", res.getMessage());
		verify(bidDAO, never()).saveBid(any());
	}

	@Test
	void testPlaceBid_AuctionNotRunning() throws Exception {
		BidRequest req = new BidRequest();
		req.setAuctionId(10L);
		req.setAmount(150.0);

		Auction a = new Auction();
		a.setStatus(AuctionStatus.FINISHED); // Đã kết thúc

		when(auctionDAO.findAuction(10L)).thenReturn(a);

		BidResponse res = bidService.placeBid(req, 2L);

		assertFalse(res.isSuccess());
		assertEquals("Phiên đấu giá chưa diễn ra hoặc đã kết thúc.", res.getMessage());
	}

	@Test
	void testPlaceBid_AuctionNotFound() throws Exception {
		BidRequest req = new BidRequest();
		req.setAuctionId(99L);
		req.setAmount(150.0);

		when(auctionDAO.findAuction(99L)).thenThrow(new Exception("AUCTION_NOT_FOUND"));

		BidResponse res = bidService.placeBid(req, 2L);

		assertFalse(res.isSuccess());
		assertEquals("Không tìm thấy phiên đấu giá.", res.getMessage());
	}

	@Test
	void testPlaceBid_AntiSniping_ExtendAuction() throws Exception {

		// ===== Arrange =====

		BidRequest req = new BidRequest();
		req.setAuctionId(10L);
		req.setAmount(200.0);

		Auction auction = new Auction();
		auction.setId(10L);
		auction.setSellerId(1L);
		auction.setStatus(AuctionStatus.RUNNING);
		auction.setCurrentPrice(100.0);

		// còn 20 giây -> phải trigger anti-sniping
		LocalDateTime oldEndTime = LocalDateTime.now().plusSeconds(20);
		auction.setEndTime(oldEndTime);

		when(auctionDAO.findAuction(10L)).thenReturn(auction);

		when(bidDAO.saveBid(any(BidTransaction.class))).thenReturn(true);

		when(auctionDAO.updateAuction(auction)).thenReturn(true);

		// ===== Act =====

		BidResponse res = bidService.placeBid(req, 2L);

		// ===== Assert =====

		assertTrue(res.isSuccess());

		assertEquals("Đặt giá thành công & Hệ thống đã tự động gia hạn phiên đấu giá!", res.getMessage());

		// endTime phải được +60s
		assertEquals(oldEndTime.plusSeconds(60), auction.getEndTime());

		// current price phải update
		assertEquals(200.0, auction.getCurrentPrice());

		// winner phải update
		assertEquals(2L, auction.getWinnerId());

		// verify save bid
		verify(bidDAO, times(1)).saveBid(any(BidTransaction.class));

		// verify update auction
		verify(auctionDAO, times(1)).updateAuction(auction);

		// verify refresh status
		verify(auctionService, times(1)).refreshStatus(10L);
	}
}