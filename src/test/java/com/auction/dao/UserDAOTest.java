package com.auction.dao;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class UserDAOTest {

	private UserDAO userDAO;

	@BeforeEach
	void setUp() {
		userDAO = new UserDAO();
	}

	@Test
	@DisplayName("Test findUsernameById - Trả về username khi có trong DB")
	void testFindUsernameById_Success() throws Exception {
		Long testId = 1L;
		String expectedUsername = "test_user";

		Connection mockConn = mock(Connection.class);
		PreparedStatement mockStmt = mock(PreparedStatement.class);
		ResultSet mockRs = mock(ResultSet.class);

		when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
		when(mockStmt.executeQuery()).thenReturn(mockRs);
		when(mockRs.next()).thenReturn(true); // Tìm thấy record
		when(mockRs.getString("username")).thenReturn(expectedUsername);

		try (MockedStatic<DatabaseConnection> mockedDbConn = Mockito.mockStatic(DatabaseConnection.class)) {
			mockedDbConn.when(DatabaseConnection::getConnection).thenReturn(mockConn);

			String result = userDAO.findUsernameById(testId);

			assertEquals(expectedUsername, result);
			verify(mockStmt).setLong(1, testId);
		}
	}

	@Test
	@DisplayName("Test findUsernameById - Trả về null khi truyền id null")
	void testFindUsernameById_NullInput() {
		String result = userDAO.findUsernameById(null);
		assertNull(result, "Nếu truyền null ID, hàm phải trả về null để caller tự fallback");
	}

	@Test
	@DisplayName("Test authenticate - Đăng nhập sai mật khẩu ném Exception")
	void testAuthenticate_InvalidPassword() throws Exception {
		Connection mockConn = mock(Connection.class);
		PreparedStatement mockStmt = mock(PreparedStatement.class);
		ResultSet mockRs = mock(ResultSet.class);

		when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
		when(mockStmt.executeQuery()).thenReturn(mockRs);
		when(mockRs.next()).thenReturn(true);
		when(mockRs.getString("password")).thenReturn("correct_password");

		try (MockedStatic<DatabaseConnection> mockedDbConn = Mockito.mockStatic(DatabaseConnection.class)) {
			mockedDbConn.when(DatabaseConnection::getConnection).thenReturn(mockConn);

			Exception exception = assertThrows(Exception.class, () -> {
				userDAO.authenticate("user1", "wrong_password");
			});

			assertEquals("INVALID_PASSWORD", exception.getMessage());
		}
	}
}