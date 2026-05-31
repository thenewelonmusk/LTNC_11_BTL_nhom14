package com.auction.dao;

import com.auction.model.BidTransaction;
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

class BidDAOTest {

	private BidDAO bidDAO;

	@BeforeEach
	void setUp() {
		bidDAO = new BidDAO();
	}

	@Test
	@DisplayName("Test saveBid - Lưu thành công bên trong Transaction")
	void testSaveBid_InTransaction_Success() throws Exception {
		BidTransaction bid = new BidTransaction();
		bid.setAuctionId(5L);
		bid.setBidderId(12L);
		bid.setAmount(1500.0);

		Connection mockConn = mock(Connection.class);
		PreparedStatement mockStmt = mock(PreparedStatement.class);
		ResultSet mockRs = mock(ResultSet.class);

		when(mockConn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockStmt);
		when(mockStmt.executeUpdate()).thenReturn(1);
		when(mockStmt.getGeneratedKeys()).thenReturn(mockRs);
		when(mockRs.next()).thenReturn(true);
		when(mockRs.getLong(1)).thenReturn(77L);

		try (MockedStatic<DatabaseConnection> mockedDb = mockStatic(DatabaseConnection.class)) {
			mockedDb.when(DatabaseConnection::isInTransaction).thenReturn(true);
			mockedDb.when(DatabaseConnection::getConnection).thenReturn(mockConn);

			boolean isSaved = bidDAO.saveBid(bid);

			assertTrue(isSaved);
			assertEquals(77L, bid.getId());
			verify(mockConn, never()).close();
		}
	}
}