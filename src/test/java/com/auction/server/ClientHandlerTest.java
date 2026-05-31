package com.auction.server;

import com.auction.dao.AuctionDAO;
import com.auction.dao.BidDAO;
import com.auction.dao.ItemDAO;
import com.auction.dao.UserDAO;
import com.auction.service.AuctionService;
import com.auction.service.BidService;
import com.auction.service.ItemService;
import com.auction.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Socket;

import static org.mockito.Mockito.*;

class ClientHandlerTest {

	@Test
	@DisplayName("Test ClientHandler khởi tạo Socket Streams thành công qua Injection")
	void testClientHandlerInitializationWithInjection() throws Exception {
		Socket mockSocket = mock(Socket.class);
		String dummyInput = "{\"action\":\"UNKNOWN\", \"data\":{}}\n";
		InputStream mockInputStream = new ByteArrayInputStream(dummyInput.getBytes());
		ByteArrayOutputStream mockOutputStream = new ByteArrayOutputStream();

		when(mockSocket.getInputStream()).thenReturn(mockInputStream);
		when(mockSocket.getOutputStream()).thenReturn(mockOutputStream);

		UserDAO mockUserDAO = mock(UserDAO.class);
		ItemDAO mockItemDAO = mock(ItemDAO.class);
		AuctionDAO mockAuctionDAO = mock(AuctionDAO.class);
		BidDAO mockBidDAO = mock(BidDAO.class);

		UserService mockUserService = mock(UserService.class);
		ItemService mockItemService = mock(ItemService.class);
		AuctionService mockAuctionService = mock(AuctionService.class);
		BidService mockBidService = mock(BidService.class);

		ClientHandler handler = new ClientHandler(mockSocket, mockUserDAO, mockItemDAO, mockAuctionDAO, mockBidDAO,
				mockUserService, mockItemService, mockAuctionService, mockBidService);

		verify(mockSocket, times(1)).getInputStream();
		verify(mockSocket, times(1)).getOutputStream();
	}
}