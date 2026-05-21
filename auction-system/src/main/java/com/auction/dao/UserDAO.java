package com.auction.dao;

import com.auction.model.user.*; // Đảm bảo import đúng package chứa Bidder, Seller, Admin
import java.sql.*;

public class UserDAO {
	// Nên ném Exception cụ thể để Service bắt được
	public User authenticate(String username, String password) throws Exception {
		String sql = "SELECT * FROM users WHERE username = ?";

		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {

			stmt.setString(1, username);
			ResultSet rs = stmt.executeQuery();

			if (rs.next()) {
				String dbPassword = rs.getString("password");
				if (dbPassword.equals(password)) {
					Long id = rs.getLong("id");
					String role = rs.getString("role");

					switch (role.toUpperCase()) {
						case "BIDDER" :
							return new Bidder(id, username, password);
						case "SELLER" :
							return new Seller(id, username, password);
						case "ADMIN" :
							return new Admin(id, username, password);
						default :
							throw new Exception("UNKNOWN_ROLE");
					}
				} else {
					throw new Exception("INVALID_PASSWORD");
				}
			} else {
				throw new Exception("INVALID_USERNAME");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new Exception("DATABASE_ERROR");
		}
	}

	public boolean registerUser(String username, String password, String role) throws Exception {
		String checkSql = "SELECT id FROM users WHERE username = ?";
		String insertSql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

		try (Connection conn = DatabaseConnection.getConnection()) {
			try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
				checkStmt.setString(1, username);

				ResultSet rs = checkStmt.executeQuery();
				if (rs.next()) {
					throw new Exception("USERNAME_EXIST");
				}
			}

			try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
				insertStmt.setString(1, username);
				insertStmt.setString(2, password);
				insertStmt.setString(3, role.toUpperCase());

				int rowsAffected = insertStmt.executeUpdate();
				if (rowsAffected > 0) {
					return true;
				}
				throw new Exception("SAVE_FAILED");
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new Exception("DATABASE_ERROR");
		}
	}
}