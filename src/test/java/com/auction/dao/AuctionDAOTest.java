package com.auction.dao;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuctionDAOTest {

	private AuctionDAO auctionDAO;

	@BeforeEach
	void setUp() {
		auctionDAO = new AuctionDAO();
	}

	@Test
	@DisplayName("Test createAuction - Thành công (Không nằm trong Transaction)")
	void testCreateAuction_NotInTransaction_Success() throws Exception {
		Auction a = new Auction();
		a.setItemId(10L);
		a.setSellerId(2L);
		a.setStartingPrice(500.0);
		a.setCurrentPrice(500.0);
		a.setStartTime(LocalDateTime.now());
		a.setEndTime(LocalDateTime.now().plusDays(1));
		a.setStatus(AuctionStatus.OPEN);

		Connection mockConn = mock(Connection.class);
		PreparedStatement mockStmt = mock(PreparedStatement.class);
		ResultSet mockRs = mock(ResultSet.class);

		when(mockConn.prepareStatement(anyString(), eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(mockStmt);
		when(mockStmt.executeUpdate()).thenReturn(1);
		when(mockStmt.getGeneratedKeys()).thenReturn(mockRs);
		when(mockRs.next()).thenReturn(true);
		when(mockRs.getLong(1)).thenReturn(101L); // Trả về ID phiên đấu giá 101

		try (MockedStatic<DatabaseConnection> mockedDb = mockStatic(DatabaseConnection.class)) {
			mockedDb.when(DatabaseConnection::isInTransaction).thenReturn(false);
			mockedDb.when(DatabaseConnection::getConnection).thenReturn(mockConn);

			boolean isCreated = auctionDAO.createAuction(a);

			assertTrue(isCreated);
			assertEquals(101L, a.getId());
		}
	}
}