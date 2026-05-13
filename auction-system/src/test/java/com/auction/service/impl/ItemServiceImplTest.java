package com.auction.service.impl;

import com.auction.dao.ItemDAO;
import com.auction.dto.ItemRequest;
import com.auction.dto.ItemResponse;
import com.auction.model.item.Item;
import com.auction.service.impl.ItemServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ItemServiceImplTest {

    @Mock
    private ItemDAO itemDAO;

    private ItemServiceImpl itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemServiceImpl(itemDAO);
    }

    // ================= CREATE ITEM TESTS =================
//
//    @Test
//    void testCreateItem_Success() throws Exception {
//        ItemRequest req = new ItemRequest();
//        req.setName("Sản phẩm xịn");
//        req.setType("ELECTRONICS");
//        req.setStartingPrice(100.0);
//
//        when(itemDAO.createItem(any())).thenReturn(true);
//
//        ItemResponse res = itemService.createItem(req, 1L);
//
//        assertTrue(res.isSuccess());
//        assertEquals("Tạo sản phẩm thành công.", res.getMessage());
//        assertEquals(1L, req.getSellerId()); // Kiểm tra sellerId đã được gán
//    }

    @Test
    void testCreateItem_FailValidation() {
        ItemRequest req = new ItemRequest();
        req.setName(""); // Tên trống

        ItemResponse res = itemService.createItem(req, 1L);

        assertFalse(res.isSuccess());
        assertEquals("Tên sản phẩm bắt buộc.", res.getMessage());
    }

    @Test
    void testCreateItem_DatabaseError() throws Exception {
        ItemRequest req = new ItemRequest();
        req.setName("Bàn phím");
        req.setType("ELECTRONICS"); // Cần type đúng để qua Factory

        when(itemDAO.createItem(any())).thenThrow(new Exception("DATABASE_ERROR"));

        ItemResponse res = itemService.createItem(req, 1L);

        assertFalse(res.isSuccess());
        assertEquals("DATABASE_ERROR", res.getMessage());
    }

    // ================= UPDATE ITEM TESTS =================

    @Test
    void testUpdateItem_Success() throws Exception {
        ItemRequest req = new ItemRequest();
        req.setName("Sửa tên");
        req.setType("ART");

        Item mockItem = mock(Item.class);
        when(mockItem.getSellerId()).thenReturn(1L); // Cùng chủ
        when(itemDAO.findItem(100L)).thenReturn(mockItem);
        when(itemDAO.updateItem(any())).thenReturn(true);

        ItemResponse res = itemService.updateItem(100L, req, 1L);

        assertTrue(res.isSuccess());
        assertEquals("Cập nhật thành công.", res.getMessage());
    }

    @Test
    void testUpdateItem_NotOwner() throws Exception {
        ItemRequest req = new ItemRequest();
        req.setName("Sửa trộm");

        Item mockItem = mock(Item.class);
        when(mockItem.getSellerId()).thenReturn(2L); // Chủ là người ID 2
        when(itemDAO.findItem(100L)).thenReturn(mockItem); // Mình là ID 1

        ItemResponse res = itemService.updateItem(100L, req, 1L);

        assertFalse(res.isSuccess());
        assertEquals("Bạn không phải chủ sản phẩm này.", res.getMessage());
    }

    @Test
    void testUpdateItem_NotFound() throws Exception {
        ItemRequest req = new ItemRequest();
        req.setName("Update");

        when(itemDAO.findItem(100L)).thenThrow(new Exception("ITEM_NOT_FOUND"));

        ItemResponse res = itemService.updateItem(100L, req, 1L);

        assertFalse(res.isSuccess());
        assertEquals("Không tìm thấy sản phẩm.", res.getMessage());
    }

    // ================= DELETE ITEM TESTS =================

    @Test
    void testDeleteItem_Success() throws Exception {
        Item mockItem = mock(Item.class);
        when(mockItem.getSellerId()).thenReturn(1L);
        when(itemDAO.findItem(10L)).thenReturn(mockItem);
        when(itemDAO.deleteItem(10L)).thenReturn(true);

        ItemResponse res = itemService.deleteItem(10L, 1L);

        assertTrue(res.isSuccess());
        assertEquals("Xóa sản phẩm thành công.", res.getMessage());
    }

    @Test
    void testDeleteItem_NotOwner() throws Exception {
        Item mockItem = mock(Item.class);
        when(mockItem.getSellerId()).thenReturn(99L);
        when(itemDAO.findItem(10L)).thenReturn(mockItem);

        ItemResponse res = itemService.deleteItem(10L, 1L);

        assertFalse(res.isSuccess());
        assertEquals("Bạn không phải chủ sản phẩm này.", res.getMessage());
        verify(itemDAO, never()).deleteItem(anyLong()); // Đảm bảo DAO xóa không được gọi
    }

    // ================= FIND ITEMS TESTS =================

    @Test
    void testFindByType_Valid() {
        itemService.findByType("electronics");
        verify(itemDAO).findByType("ELECTRONICS"); // Phải được in hoa
    }

    @Test
    void testFindByType_Invalid() {
        List<Item> result = itemService.findByType("");
        assertTrue(result.isEmpty());
        verify(itemDAO, never()).findByType(anyString());
    }

    @Test
    void testFindBySeller_Valid() {
        itemService.findBySeller(1L);
        verify(itemDAO).findBySeller(1L);
    }

    @Test
    void testFindBySeller_Invalid() {
        List<Item> result = itemService.findBySeller(-5L);
        assertTrue(result.isEmpty());
        verify(itemDAO, never()).findBySeller(anyLong());
    }
}