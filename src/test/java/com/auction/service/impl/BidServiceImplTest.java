package com.auction.service.impl;

import com.auction.dao.AuctionDAO;
import com.auction.dao.BidDAO;
import com.auction.dto.BidRequest;
import com.auction.dto.BidResponse;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.BidTransaction;
import com.auction.service.AuctionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidServiceImplTest {

	@Mock
	private BidDAO bidDAO;

	@Mock
	private AuctionDAO auctionDAO;

	@Mock
	private AuctionService auctionService;

	private BidServiceImpl bidService;

	@BeforeEach
	void setUp() {
		bidService = new BidServiceImpl(bidDAO, auctionDAO, auctionService);
	}

	@Test
	void placeBid_shouldSucceed_whenBidIsValid() throws Exception {
		BidRequest req = new BidRequest();
		req.setAuctionId(10L);
		req.setAmount(150.0);

		Auction auction = new Auction();
		auction.setId(10L);
		auction.setSellerId(1L);
		auction.setStatus(AuctionStatus.RUNNING);
		auction.setCurrentPrice(100.0);
		auction.setEndTime(LocalDateTime.now().plusMinutes(5));

		when(auctionDAO.findAuction(10L)).thenReturn(auction);
		when(bidDAO.saveBid(any(BidTransaction.class))).thenReturn(true);
		when(auctionDAO.updateAuction(any(Auction.class))).thenReturn(true);

		BidResponse res = bidService.placeBid(req, 2L);

		assertTrue(res.isSuccess());
		assertEquals("Đặt giá thành công.", res.getMessage());
		assertNotNull(res.getBid());
		assertEquals(150.0, res.getBid().getAmount(), 0.0001);
		assertEquals(150.0, res.getCurrentPrice(), 0.0001);
		assertEquals(150.0, auction.getCurrentPrice(), 0.0001);
		assertEquals(2L, auction.getWinnerId());

		ArgumentCaptor<BidTransaction> bidCaptor = ArgumentCaptor.forClass(BidTransaction.class);
		verify(auctionService).refreshStatus(10L);
		verify(bidDAO).saveBid(bidCaptor.capture());
		verify(auctionDAO, atLeastOnce()).updateAuction(any(Auction.class));

		BidTransaction savedBid = bidCaptor.getValue();
		assertEquals(10L, savedBid.getAuctionId());
		assertEquals(2L, savedBid.getBidderId());
		assertEquals(150.0, savedBid.getAmount(), 0.0001);
		assertFalse(savedBid.isAutoBid());
	}

	@Test
	void placeBid_shouldExtendAuction_whenBidIsInLast30Seconds() throws Exception {
		BidRequest req = new BidRequest();
		req.setAuctionId(11L);
		req.setAmount(250.0);

		Auction auction = new Auction();
		auction.setId(11L);
		auction.setSellerId(1L);
		auction.setStatus(AuctionStatus.RUNNING);
		auction.setCurrentPrice(200.0);
		LocalDateTime oldEndTime = LocalDateTime.now().plusSeconds(20);
		auction.setEndTime(oldEndTime);

		when(auctionDAO.findAuction(11L)).thenReturn(auction);
		when(bidDAO.saveBid(any(BidTransaction.class))).thenReturn(true);
		when(auctionDAO.updateAuction(any(Auction.class))).thenReturn(true);

		BidResponse res = bidService.placeBid(req, 2L);

		assertTrue(res.isSuccess());
		assertNotNull(res.getMessage());
		assertTrue(res.getMessage().contains("Đặt giá thành công"));
		assertTrue(res.getMessage().contains("gia hạn"));
		assertEquals(250.0, res.getCurrentPrice(), 0.0001);
		assertEquals(250.0, auction.getCurrentPrice(), 0.0001);
		assertEquals(2L, auction.getWinnerId());
		assertTrue(auction.getEndTime().isAfter(oldEndTime));
		assertTrue(auction.getEndTime().isAfter(oldEndTime.plusSeconds(50)));

		verify(auctionService).refreshStatus(11L);
		verify(bidDAO).saveBid(any(BidTransaction.class));
		verify(auctionDAO, atLeastOnce()).updateAuction(auction);
	}

	@Test
	void placeBid_shouldReject_whenSellerBidsOnOwnAuction() throws Exception {
		BidRequest req = new BidRequest();
		req.setAuctionId(10L);
		req.setAmount(150.0);

		Auction auction = new Auction();
		auction.setSellerId(1L);
		auction.setStatus(AuctionStatus.RUNNING);
		auction.setCurrentPrice(100.0);
		auction.setEndTime(LocalDateTime.now().plusMinutes(5));

		when(auctionDAO.findAuction(10L)).thenReturn(auction);

		BidResponse res = bidService.placeBid(req, 1L);

		assertFalse(res.isSuccess());
		assertEquals("Chủ sản phẩm không thể đấu giá.", res.getMessage());
		assertEquals(100.0, res.getCurrentPrice(), 0.0001);

		verify(auctionService).refreshStatus(10L);
		verify(bidDAO, never()).saveBid(any());
		verify(auctionDAO, never()).updateAuction(any());
	}

	@Test
	void placeBid_shouldReject_whenBidIsTooLow() throws Exception {
		BidRequest req = new BidRequest();
		req.setAuctionId(10L);
		req.setAmount(100.5);

		Auction auction = new Auction();
		auction.setSellerId(1L);
		auction.setStatus(AuctionStatus.RUNNING);
		auction.setCurrentPrice(100.0);
		auction.setEndTime(LocalDateTime.now().plusMinutes(5));

		when(auctionDAO.findAuction(10L)).thenReturn(auction);

		BidResponse res = bidService.placeBid(req, 2L);

		assertFalse(res.isSuccess());
		assertEquals("Giá đặt phải cao hơn giá hiện tại.", res.getMessage());
		assertEquals(100.0, res.getCurrentPrice(), 0.0001);

		verify(auctionService).refreshStatus(10L);
		verify(bidDAO, never()).saveBid(any());
		verify(auctionDAO, never()).updateAuction(any());
	}

	@Test
	void placeBid_shouldReject_whenAuctionIsNotRunning() throws Exception {
		BidRequest req = new BidRequest();
		req.setAuctionId(10L);
		req.setAmount(150.0);

		Auction auction = new Auction();
		auction.setSellerId(1L);
		auction.setStatus(AuctionStatus.FINISHED);
		auction.setCurrentPrice(100.0);
		auction.setEndTime(LocalDateTime.now().minusMinutes(1));

		when(auctionDAO.findAuction(10L)).thenReturn(auction);

		BidResponse res = bidService.placeBid(req, 2L);

		assertFalse(res.isSuccess());
		assertEquals("Phiên đấu giá chưa diễn ra hoặc đã kết thúc.", res.getMessage());
		assertEquals(100.0, res.getCurrentPrice(), 0.0001);

		verify(auctionService).refreshStatus(10L);
		verify(bidDAO, never()).saveBid(any());
		verify(auctionDAO, never()).updateAuction(any());
	}

	@Test
	void placeBid_shouldReject_whenRequestIsInvalid() {
		BidResponse res1 = bidService.placeBid(null, 2L);

		BidRequest req = new BidRequest();
		req.setAuctionId(null);
		req.setAmount(150.0);
		BidResponse res2 = bidService.placeBid(req, 2L);

		BidRequest req2 = new BidRequest();
		req2.setAuctionId(10L);
		req2.setAmount(0);
		BidResponse res3 = bidService.placeBid(req2, 2L);

		assertFalse(res1.isSuccess());
		assertFalse(res2.isSuccess());
		assertFalse(res3.isSuccess());
		assertEquals("Yêu cầu không hợp lệ.", res1.getMessage());
		assertEquals("Yêu cầu không hợp lệ.", res2.getMessage());
		assertEquals("Số tiền không hợp lệ.", res3.getMessage());

		verifyNoInteractions(auctionService, auctionDAO, bidDAO);
	}

	@Test
	void placeBid_shouldReturnAuctionNotFound_whenDaoThrowsThatError() throws Exception {
		BidRequest req = new BidRequest();
		req.setAuctionId(99L);
		req.setAmount(150.0);

		when(auctionDAO.findAuction(99L)).thenThrow(new Exception("AUCTION_NOT_FOUND"));

		BidResponse res = bidService.placeBid(req, 2L);

		assertFalse(res.isSuccess());
		assertEquals("Không tìm thấy phiên đấu giá.", res.getMessage());
		assertEquals(0.0, res.getCurrentPrice(), 0.0001);

		verify(auctionService).refreshStatus(99L);
		verify(bidDAO, never()).saveBid(any());
		verify(auctionDAO, never()).updateAuction(any());
	}

	@Test
	void placeBid_shouldReturnDatabaseErrorMessage_whenSaveBidFails() throws Exception {
		BidRequest req = new BidRequest();
		req.setAuctionId(10L);
		req.setAmount(150.0);

		Auction auction = new Auction();
		auction.setSellerId(1L);
		auction.setStatus(AuctionStatus.RUNNING);
		auction.setCurrentPrice(100.0);
		auction.setEndTime(LocalDateTime.now().plusMinutes(5));

		when(auctionDAO.findAuction(10L)).thenReturn(auction);
		when(bidDAO.saveBid(any(BidTransaction.class))).thenThrow(new Exception("DATABASE_ERROR"));

		BidResponse res = bidService.placeBid(req, 2L);

		assertFalse(res.isSuccess());
		assertEquals("DATABASE_ERROR", res.getMessage());
		assertEquals(0.0, res.getCurrentPrice(), 0.0001);

		verify(auctionService).refreshStatus(10L);
		verify(bidDAO).saveBid(any(BidTransaction.class));
		verify(auctionDAO, never()).updateAuction(any());
	}

	@Test
	void getBidsByAuction_shouldDelegateToDao() {
		BidTransaction bid1 = new BidTransaction();
		bid1.setId(1L);
		BidTransaction bid2 = new BidTransaction();
		bid2.setId(2L);

		when(bidDAO.findByAuctionId(10L)).thenReturn(List.of(bid1, bid2));

		List<BidTransaction> result = bidService.getBidsByAuction(10L);

		assertEquals(2, result.size());
		assertEquals(1L, result.get(0).getId());
		assertEquals(2L, result.get(1).getId());
		verify(bidDAO).findByAuctionId(10L);
	}

	@Test
	void getBidsByBidder_shouldDelegateToDao() {
		BidTransaction bid = new BidTransaction();
		bid.setId(7L);

		when(bidDAO.findByBidderId(2L)).thenReturn(List.of(bid));

		List<BidTransaction> result = bidService.getBidsByBidder(2L);

		assertEquals(1, result.size());
		assertEquals(7L, result.get(0).getId());
		verify(bidDAO).findByBidderId(2L);
	}

	@Test
	void getHighestBid_shouldDelegateToDao() {
		BidTransaction highest = new BidTransaction();
		highest.setId(99L);

		when(bidDAO.findHighestBidByAuction(10L)).thenReturn(highest);

		BidTransaction result = bidService.getHighestBid(10L);

		assertNotNull(result);
		assertEquals(99L, result.getId());
		verify(bidDAO).findHighestBidByAuction(10L);
	}
}