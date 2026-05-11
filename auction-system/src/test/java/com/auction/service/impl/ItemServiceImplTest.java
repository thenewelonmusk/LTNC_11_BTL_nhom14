package com.auction.service.impl;

import com.auction.dto.ItemRequest;
import com.auction.dto.ItemResponse;
import com.auction.model.item.Electronics;
import com.auction.model.item.Item;
import com.auction.repository.ItemRepository;
import com.auction.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;


@DisplayName("ItemServiceImpl Test")
public class ItemServiceImplTest {

    // Test data constants
    private static final Long SELLER_ID = 1L;
    private static final Long OTHER_SELLER_ID = 999L;
    private static final Long ITEM_ID = 10L;

    private ItemRepository itemRepository;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemRepository = mock(ItemRepository.class);
        itemService = new ItemServiceImpl(itemRepository);
    }

    //create item
    @Test
    @DisplayName("createItem: valid Electronics returns success")
    void createItem_valid_returnsSuccess() {
        ItemRequest req = new ItemRequest();
        req.setType("ELECTRONICS");
        req.setName("iPhone 15");
        req.setDescription("New phone");
        req.setDeviceBrand("Apple");
        req.setWarrantyMonths(12);

        when(itemRepository.save(any(Item.class))).thenReturn(true);

        ItemResponse response = itemService.createItem(req, SELLER_ID);

        assertTrue(response.isSuccess());
        assertNotNull(response.getItem());
        assertEquals("iPhone 15",response.getItem().getName());
        assertEquals(SELLER_ID,response.getItem().getSellerId());
        verify(itemRepository, times(1)).save(any(Item.class));
    }

    @Test
    @DisplayName("createItem: invalid name returns failure")
    void createItem_invalid_returnsFailure() {
        ItemRequest req = new ItemRequest();
        req.setType("art");
        req.setName("");                          // tên rỗng
        req.setArtist("George Orwell");
        req.setYear(1984);

        ItemResponse response = itemService.createItem(req,SELLER_ID);

        assertFalse(response.isSuccess());
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateItem: owner updates successfully")
    void updateItem_valid_returnsSuccess() {
        Electronics existing = new Electronics("Apple",12);
        existing.setId(ITEM_ID);
        existing.setSellerId(SELLER_ID);
        existing.setName("iPhone 15");

        when(itemRepository.findById(ITEM_ID)).thenReturn(existing);
        when(itemRepository.save(any(Item.class))).thenReturn(true);

        ItemRequest request = new ItemRequest();
        request.setDescription("new desc");
        request.setName("iPhone 15");

        ItemResponse response = itemService.updateItem(ITEM_ID,request,SELLER_ID);

        assertTrue(response.isSuccess());
        assertEquals("new desc",response.getItem().getDescription());
        verify(itemRepository,times(1)).save(any(Item.class));
    }

    @Test
    @DisplayName("updateItem: not owner returns failure")
    void updateItem_invalid_returnsFailure() {
        Electronics existing = new Electronics("Apple", 12);
        existing.setId(ITEM_ID);
        existing.setSellerId(SELLER_ID);

        when(itemRepository.findById(ITEM_ID)).thenReturn(existing);

        ItemRequest req = new ItemRequest();
        req.setType("ELECTRONICS");
        req.setName("Hacker name");

        ItemResponse response = itemService.updateItem(ITEM_ID, req, OTHER_SELLER_ID);

        assertFalse(response.isSuccess());
        verify(itemRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteItem: owner deletes successfully")
    void deleteItem_byOwner_returnsSuccess() {
        Electronics existing = new Electronics("Apple", 12);
        existing.setId(ITEM_ID);
        existing.setSellerId(SELLER_ID);

        when(itemRepository.findById(ITEM_ID)).thenReturn(existing);
        when(itemRepository.deleteById(ITEM_ID)).thenReturn(true);

        ItemResponse response = itemService.deleteItem(ITEM_ID, SELLER_ID);

        assertTrue(response.isSuccess());
        verify(itemRepository, times(1)).deleteById(ITEM_ID);
    }

    @Test
    @DisplayName("getItemById: not found returns failure")
    void getItemById_notFound_returnsFailure() {
        when(itemRepository.findById(ITEM_ID)).thenReturn(null);

        ItemResponse response = itemService.findItemById(ITEM_ID);

        assertFalse(response.isSuccess());
        assertNull(response.getItem());
    }
}
