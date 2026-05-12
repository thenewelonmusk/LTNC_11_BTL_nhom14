package com.auction.service.impl;

import com.auction.dao.AuctionDAO;
import com.auction.dao.ItemDAO;
import com.auction.dto.AuctionRequest;
import com.auction.dto.AuctionResponse;
import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.item.Item;
import com.auction.service.impl.AuctionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuctionServiceImplTest {

    @Mock private AuctionDAO auctionDAO;
    @Mock private ItemDAO itemDAO;

    private AuctionServiceImpl auctionService;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionServiceImpl(auctionDAO, itemDAO);
    }

    // ================= OPEN AUCTION TESTS =================

    @Test
    void testOpenAuction_Success() throws Exception {
        AuctionRequest req = new AuctionRequest();
        req.setItemId(10L);
        req.setStartingPrice(1000.0);
        req.setStartTime(LocalDateTime.now().plusDays(1)); // Ngày mai
        req.setEndTime(LocalDateTime.now().plusDays(2));

        Item mockItem = mock(Item.class);
        when(mockItem.getSellerId()).thenReturn(1L);

        when(itemDAO.findItem(10L)).thenReturn(mockItem);
        when(auctionDAO.findByItemId(10L)).thenReturn(List.of()); // Chưa có phiên nào
        when(auctionDAO.createAuction(any(Auction.class))).thenReturn(true);

        AuctionResponse res = auctionService.openAuction(req, 1L);

        assertTrue(res.isSuccess());
        assertEquals("Mở phiên đấu giá thành công.", res.getMessage());
        assertEquals(AuctionStatus.OPEN, res.getAuction().getStatus());
    }

    @Test
    void testOpenAuction_NotOwner() throws Exception {
        AuctionRequest req = new AuctionRequest();
        req.setItemId(10L);
        req.setStartingPrice(100.0);
        req.setStartTime(LocalDateTime.now().plusHours(1));
        req.setEndTime(LocalDateTime.now().plusHours(2));

        Item mockItem = mock(Item.class);
        when(mockItem.getSellerId()).thenReturn(2L); // Chủ là ID 2
        when(itemDAO.findItem(10L)).thenReturn(mockItem);

        AuctionResponse res = auctionService.openAuction(req, 1L); // Mình là ID 1

        assertFalse(res.isSuccess());
        assertEquals("Bạn không phải chủ sản phẩm này.", res.getMessage());
        verify(auctionDAO, never()).createAuction(any());
    }

    @Test
    void testOpenAuction_AlreadyRunning() throws Exception {
        AuctionRequest req = new AuctionRequest();
        req.setItemId(10L);
        req.setStartingPrice(100.0);
        req.setStartTime(LocalDateTime.now().plusHours(1));
        req.setEndTime(LocalDateTime.now().plusHours(2));

        Item mockItem = mock(Item.class);
        when(mockItem.getSellerId()).thenReturn(1L);
        when(itemDAO.findItem(10L)).thenReturn(mockItem);

        Auction existingAuction = new Auction();
        existingAuction.setStatus(AuctionStatus.RUNNING); // Đang có phiên chạy
        when(auctionDAO.findByItemId(10L)).thenReturn(List.of(existingAuction));

        AuctionResponse res = auctionService.openAuction(req, 1L);

        assertFalse(res.isSuccess());
        assertEquals("Sản phẩm đã có phiên đấu giá đang mở.", res.getMessage());
    }

    // ================= CANCEL AUCTION TESTS =================

    @Test
    void testCancelAuction_Success() throws Exception {
        Auction a = new Auction();
        a.setSellerId(1L);
        a.setStatus(AuctionStatus.OPEN);

        when(auctionDAO.findAuction(100L)).thenReturn(a);
        when(auctionDAO.updateAuction(a)).thenReturn(true);

        AuctionResponse res = auctionService.cancelAuction(100L, 1L);

        assertTrue(res.isSuccess());
        assertEquals(AuctionStatus.CANCELED, a.getStatus());
    }

    @Test
    void testCancelAuction_CannotCancelFinished() throws Exception {
        Auction a = new Auction();
        a.setSellerId(1L);
        a.setStatus(AuctionStatus.FINISHED); // Đã kết thúc

        when(auctionDAO.findAuction(100L)).thenReturn(a);

        AuctionResponse res = auctionService.cancelAuction(100L, 1L);

        assertFalse(res.isSuccess());
        assertEquals("Không thể hủy phiên đã kết thúc.", res.getMessage());
    }

    // ================= FINISH AUCTION TESTS =================

    @Test
    void testFinishAuction_StillRunning() throws Exception {
        Auction a = new Auction();
        a.setEndTime(LocalDateTime.now().plusHours(1)); // Chưa đến hạn

        when(auctionDAO.findAuction(100L)).thenReturn(a);

        AuctionResponse res = auctionService.finishAuction(100L);

        assertFalse(res.isSuccess());
        assertEquals("Phiên đấu giá chưa kết thúc.", res.getMessage());
    }
}