package com.auction.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Quản lý kết nối database với HikariCP Connection Pool - Tăng hiệu suất bằng
 * cách tái sử dụng connections - Giải quyết vấn đề "Too many connections" từ
 * MySQL - Support transactions với ThreadLocal
 */
public class DatabaseConnection {
	// HikariCP DataSource - quản lý pool của connections
	private static HikariDataSource hikariDataSource;

	// ThreadLocal để quản lý connection per-thread với transaction support
	private static final ThreadLocal<Connection> transactionConnection = new ThreadLocal<>();

	// Static initializer - khởi tạo connection pool một lần
	static {
		initializePool();
	}

	/**
	 * Khởi tạo HikariCP Connection Pool
	 */
	private static void initializePool() {
		try {
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(
					"jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true");
			config.setUsername("root");
			config.setPassword("password");

			// Cấu hình pool
			config.setMaximumPoolSize(10); // Tối đa 10 connections trong pool
			config.setMinimumIdle(5); // Tối thiểu 5 connections rảnh
			config.setConnectionTimeout(5000); // Timeout 5 giây khi chờ connection
			config.setIdleTimeout(600000); // Đóng connection nếu idle 10 phút
			config.setMaxLifetime(1800000); // Tuổi tối đa của connection: 30 phút
			config.setAutoCommit(true); // Auto-commit mặc định (transaction sẽ tắt)

			// FIX: Tăng leak detection threshold để tránh closing connection sớm
			// Giảm từ 60s xuống để phát hiện leak, nhưng vẫn đủ cho transaction dài
			config.setLeakDetectionThreshold(120000); // 120 giây (tăng từ 60)

			// FIX: Thêm connection test queries để kiểm tra health
			config.setConnectionTestQuery("SELECT 1");
			config.setValidationTimeout(5000);

			hikariDataSource = new HikariDataSource(config);
			System.out.println("[✓] HikariCP Connection Pool khởi tạo thành công!");
			System.out.println("    - Max connections: " + config.getMaximumPoolSize());
			System.out.println("    - Min idle: " + config.getMinimumIdle());
			System.out.println("    - Leak detection threshold: " + config.getLeakDetectionThreshold() + "ms");

		} catch (Exception e) {
			System.err.println("[✗] Lỗi khởi tạo HikariCP Connection Pool!");
			e.printStackTrace();
			throw new RuntimeException("Failed to initialize HikariCP", e);
		}
	}

	/**
	 * Lấy connection từ pool - Nếu đang trong transaction, trả về transaction
	 * connection - Nếu không, lấy từ pool (tái sử dụng nếu có, nếu không thì tạo
	 * mới)
	 */
	public static Connection getConnection() {
		try {
			// Nếu đang trong transaction, trả về connection từ ThreadLocal
			Connection conn = transactionConnection.get();
			if (conn != null && !conn.isClosed()) {
				return conn;
			}

			// Lấy connection từ HikariCP pool
			if (hikariDataSource != null) {
				return hikariDataSource.getConnection();
			} else {
				System.err.println("LỖI: HikariCP DataSource chưa khởi tạo!");
				return null;
			}
		} catch (SQLException e) {
			System.err.println("LỖI: Không thể lấy connection từ pool!");
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Bắt đầu transaction: lấy connection từ pool, tắt auto-commit Tất cả
	 * getConnection() gọi trong thread này sẽ dùng connection này
	 */
	public static Connection beginTransaction() throws SQLException {
		try {
			Connection conn = hikariDataSource.getConnection();
			conn.setAutoCommit(false);
			transactionConnection.set(conn);
			System.out.println("[*] Transaction bắt đầu");
			return conn;
		} catch (SQLException e) {
			System.err.println("LỖI: Không thể bắt đầu transaction!");
			throw e;
		}
	}

	/**
	 * Commit transaction và cleanup FIX: Kiểm tra xem connection có đóng không
	 * trước khi commit
	 */
	public static void commit() throws SQLException {
		Connection conn = transactionConnection.get();
		if (conn != null) {
			try {
				// Kiểm tra connection trước khi commit - nếu đóng thì khôi phục
				if (conn.isClosed()) {
					System.err.println("[!] CẢNH BÁO: Connection đã đóng trước commit, bỏ qua!");
					return;
				}

				conn.commit();
				System.out.println("[✓] Transaction commit thành công");
			} catch (SQLException e) {
				// Nếu commit fail, thử rollback
				try {
					if (!conn.isClosed()) {
						conn.rollback();
						System.out.println("[!] Rollback do lỗi commit: " + e.getMessage());
					}
				} catch (SQLException ignored) {
				}
				throw e;
			} finally {
				try {
					if (!conn.isClosed()) {
						conn.setAutoCommit(true);
						conn.close(); // Return connection về pool
					}
				} catch (SQLException ignored) {
				}
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
				System.out.println("[!] Transaction rollback");
			} catch (SQLException e) {
				System.err.println("LỖI rollback: " + e.getMessage());
			} finally {
				try {
					conn.setAutoCommit(true);
					conn.close(); // Return connection về pool
				} catch (SQLException e) {
					System.err.println("LỖI đóng connection: " + e.getMessage());
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
						conn.setAutoCommit(true);
						conn.close();
					}
				} catch (SQLException ignored) {
				}
			} finally {
				transactionConnection.remove();
			}
		}
	}

	/**
	 * Kiểm tra xem có đang trong transaction không
	 */
	public static boolean isInTransaction() {
		return transactionConnection.get() != null;
	}

	/**
	 * Đóng pool khi ứng dụng tắt (gọi trong shutdown hook)
	 */
	public static void closePool() {
		if (hikariDataSource != null && !hikariDataSource.isClosed()) {
			hikariDataSource.close();
			System.out.println("[✓] HikariCP Connection Pool đóng thành công");
		}
	}

	/**
	 * Lấy thông tin pool hiện tại
	 */
	public static void printPoolStats() {
		if (hikariDataSource != null) {
			System.out.println("=== HikariCP Pool Stats ===");
			System.out.println("Total connections: " + hikariDataSource.getHikariPoolMXBean().getTotalConnections());
			System.out.println("Active connections: " + hikariDataSource.getHikariPoolMXBean().getActiveConnections());
			System.out.println("Idle connections: " + hikariDataSource.getHikariPoolMXBean().getIdleConnections());
			// System.out.println("Pending threads: " +
			// hikariDataSource.getHikariPoolMXBean().getPendingThreads());
		}
	}
}