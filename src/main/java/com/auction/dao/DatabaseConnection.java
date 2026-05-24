package com.auction.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
	private static Connection connection = null;
	// ThreadLocal để quản lý connection per-thread với transaction support
	private static final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

	public static Connection getConnection() {
		try {
			// Nếu đang trong transaction, trả về connection từ ThreadLocal
			Connection conn = transactionConnection.get();
			if (conn != null && !conn.isClosed()) {
				return conn;
			}

			// Driver mới cho bản 8.0 là com.mysql.cj.jdbc.Driver
			Class.forName("com.mysql.cj.jdbc.Driver");

			// Thêm tham số timezone và tắt SSL để tránh lỗi bảo mật khi test local
			String url = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
			String user = "root";
			String pass = "password"; // Mật khẩu bạn vừa test trong CMD

			return DriverManager.getConnection(url, user, pass);
		} catch (ClassNotFoundException e) {
			System.err.println("LỖI: Không tìm thấy thư viện MySQL Connector (Driver)!");
			return null;
		} catch (SQLException e) {
			System.err.println("LỖI: Sai thông tin kết nối hoặc MySQL chưa bật!");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Bắt đầu transaction: tạo connection mới, tắt auto-commit Tất cả
	 * getConnection() gọi trong thread này sẽ dùng connection này
	 */
	public static Connection beginTransaction() throws SQLException {
		try {
			Class.forName("com.mysql.cj.jdbc.Driver");
			String url = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
			Connection conn = DriverManager.getConnection(url, "root", "password");
			conn.setAutoCommit(false);
			transactionConnection.set(conn);
			return conn;
		} catch (ClassNotFoundException e) {
			throw new SQLException("MySQL Driver not found", e);
		}
	}

	/**
	 * Commit transaction và cleanup
	 */
	public static void commit() throws SQLException {
		Connection conn = transactionConnection.get();
		if (conn != null) {
			try {
				conn.commit();
			} finally {
				conn.close();
				transactionConnection.remove();
			}
		}
	}

	/**
	 * Rollback transaction và cleanup
	 */
	public static void rollback() {
		Connection conn = transactionConnection.get();
		if (conn != null) {
			try {
				conn.rollback();
			} catch (SQLException e) {
				e.printStackTrace();
			} finally {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				transactionConnection.remove();
			}
		}
	}

	/**
	 * Cleanup transaction connection nếu có lỗi
	 */
	public static void cleanup() {
		Connection conn = transactionConnection.get();
		if (conn != null) {
			try {
				try {
					if (!conn.isClosed() && !conn.getAutoCommit()) {
						conn.rollback();
					}
				} catch (SQLException ignored) {
				}
				try {
					if (!conn.isClosed()) {
						conn.close();
					}
				} catch (SQLException ignored) {
				}
			} finally {
				transactionConnection.remove();
			}
		}
	}
}