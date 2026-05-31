package com.auction.dao;

import com.auction.dto.ItemRequest;
import com.auction.model.item.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ItemDAOTest {

	private ItemDAO itemDAO;

	@BeforeEach
	void setUp() {
		itemDAO = new ItemDAO();
	}

	@Test
	@DisplayName("Test createItem - Trả về ID tự động tăng khi lưu thành công")
	void testCreateItem_Success() throws Exception {
		ItemRequest req = new ItemRequest();
		req.setName("Laptop Dell");
		req.setDescription("Mới 99%");
		req.setType("ELECTRONICS");
		req.setStartingPrice(15000.0);
		req.setSellerId(2L);

		Connection mockConn = mock(Connection.class);
		PreparedStatement mockStmt = mock(PreparedStatement.class);
		ResultSet mockRs = mock(ResultSet.class);

		when(mockConn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockStmt);
		when(mockStmt.executeUpdate()).thenReturn(1);
		when(mockStmt.getGeneratedKeys()).thenReturn(mockRs);
		when(mockRs.next()).thenReturn(true);
		when(mockRs.getLong(1)).thenReturn(99L);

		try (MockedStatic<DatabaseConnection> mockedDb = mockStatic(DatabaseConnection.class)) {
			mockedDb.when(DatabaseConnection::getConnection).thenReturn(mockConn);

			Long generatedId = itemDAO.createItem(req);
			assertEquals(99L, generatedId);
		}
	}

	@Test
	@DisplayName("Test findItem - Trả về đối tượng Item khi tìm thấy")
	void testFindItem_Success() throws Exception {
		Connection mockConn = mock(Connection.class);
		PreparedStatement mockStmt = mock(PreparedStatement.class);
		ResultSet mockRs = mock(ResultSet.class);

		when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
		when(mockStmt.executeQuery()).thenReturn(mockRs);
		when(mockRs.next()).thenReturn(true); // Tìm thấy
		when(mockRs.getLong("id")).thenReturn(1L);
		when(mockRs.getString("name")).thenReturn("Bức tranh Picasso");
		when(mockRs.getString("category")).thenReturn("ART"); // Bắt buộc đúng loại để Factory tạo
		when(mockRs.getString("description")).thenReturn("Tranh xịn");
		when(mockRs.getDouble("starting_price")).thenReturn(100.0);
		when(mockRs.getDouble("current_price")).thenReturn(150.0);
		when(mockRs.getLong("seller_id")).thenReturn(5L);

		try (MockedStatic<DatabaseConnection> mockedDb = mockStatic(DatabaseConnection.class)) {
			mockedDb.when(DatabaseConnection::getConnection).thenReturn(mockConn);

			Item item = itemDAO.findItem(1L);
			assertNotNull(item);
			assertEquals("Bức tranh Picasso", item.getName());
			assertEquals("Art", item.getType());
		}
	}
}