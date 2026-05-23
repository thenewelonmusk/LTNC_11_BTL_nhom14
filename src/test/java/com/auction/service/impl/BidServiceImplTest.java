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
	void testPlaceBid_Success() throws Exception {
		BidRequest req = new BidRequest();
		req.setAuctionId(10L);
		req.setAmount(150.0); // Giá mới

		Auction a = new Auction();
		a.setId(10L);
		a.setSellerId(1L); // Chủ là ID 1
		a.setStatus(AuctionStatus.RUNNING);
		a.setCurrentPrice(100.0); // Giá cũ

		when(auctionDAO.findAuction(10L)).thenReturn(a);
		when(bidDAO.saveBid(any(BidTransaction.class))).thenReturn(true);
		when(auctionDAO.updateAuction(a)).thenReturn(true);

		// Act: Đặt giá bởi người mua ID = 2
		BidResponse res = bidService.placeBid(req, 2L);

		// Assert
		assertTrue(res.isSuccess());
		assertEquals("Đặt giá thành công.", res.getMessage());
		assertEquals(150.0, res.getBid().getAmount());
		assertEquals(150.0, a.getCurrentPrice()); // Đảm bảo giá phiên đã nhảy
		assertEquals(2L, a.getWinnerId()); // Đảm bảo Winner được cập nhật

		verify(auctionService, times(1)).refreshStatus(10L);
		verify(bidDAO, times(1)).saveBid(any());
		verify(auctionDAO, times(1)).updateAuction(a);
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
}