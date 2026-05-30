package com.auction.dao;

import com.auction.model.BidTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

	public boolean saveBid(BidTransaction bid) throws Exception {
		String sql = "INSERT INTO bids (auction_id, bidder_id, amount) VALUES (?, ?, ?)";

		// Nếu trong transaction, KHÔNG dùng try-with-resources để tránh đóng connection
		// sớm
		if (DatabaseConnection.isInTransaction()) {
			Connection conn = DatabaseConnection.getConnection();
			try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				stmt.setLong(1, bid.getAuctionId());
				stmt.setLong(2, bid.getBidderId());
				stmt.setDouble(3, bid.getAmount());

				int affectedRows = stmt.executeUpdate();
				if (affectedRows > 0) {
					try (ResultSet rs = stmt.getGeneratedKeys()) {
						if (rs.next()) {
							bid.setId(rs.getLong(1));
						}
					}
					return true;
				}
				throw new Exception("SAVE_FAILED");
			} catch (SQLException e) {
				e.printStackTrace();
				throw new Exception("DATABASE_ERROR");
			}
		} else {
			// Bình thường (không transaction), dùng try-with-resources
			try (Connection conn = DatabaseConnection.getConnection();
					PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				stmt.setLong(1, bid.getAuctionId());
				stmt.setLong(2, bid.getBidderId());
				stmt.setDouble(3, bid.getAmount());

				int affectedRows = stmt.executeUpdate();
				if (affectedRows > 0) {
					try (ResultSet rs = stmt.getGeneratedKeys()) {
						if (rs.next()) {
							bid.setId(rs.getLong(1));
						}
					}
					return true;
				}
				throw new Exception("SAVE_FAILED");
			} catch (SQLException e) {
				e.printStackTrace();
				throw new Exception("DATABASE_ERROR");
			}
		}
	}

	public List<BidTransaction> findByAuctionId(Long auctionId) {
		List<BidTransaction> list = new ArrayList<>();
		// FIX: trả về theo thứ tự thời gian (id ASC) để chart vẽ đúng tiến trình.
		// Việc sort theo "bid cao nhất ở đầu" để hiển thị bảng được làm ở phía client.
		String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY id ASC";

		if (DatabaseConnection.isInTransaction()) {
			Connection conn = DatabaseConnection.getConnection();
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, auctionId);
				try (ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						list.add(mapRow(rs));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try (Connection conn = DatabaseConnection.getConnection();
					PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, auctionId);
				try (ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						list.add(mapRow(rs));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	public List<BidTransaction> findByBidderId(Long bidderId) {
		List<BidTransaction> list = new ArrayList<>();
		String sql = "SELECT * FROM bids WHERE bidder_id = ? ORDER BY id DESC";

		if (DatabaseConnection.isInTransaction()) {
			Connection conn = DatabaseConnection.getConnection();
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, bidderId);
				try (ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						list.add(mapRow(rs));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try (Connection conn = DatabaseConnection.getConnection();
					PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, bidderId);
				try (ResultSet rs = stmt.executeQuery()) {
					while (rs.next()) {
						list.add(mapRow(rs));
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return list;
	}

	public BidTransaction findHighestBidByAuction(Long auctionId) {
		String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY amount DESC LIMIT 1";

		if (DatabaseConnection.isInTransaction()) {
			Connection conn = DatabaseConnection.getConnection();
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, auctionId);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return mapRow(rs);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			try (Connection conn = DatabaseConnection.getConnection();
					PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, auctionId);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return mapRow(rs);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private BidTransaction mapRow(ResultSet rs) throws SQLException {
		BidTransaction bid = new BidTransaction();
		bid.setId(rs.getLong("id"));
		bid.setAuctionId(rs.getLong("auction_id"));
		bid.setBidderId(rs.getLong("bidder_id"));
		bid.setAmount(rs.getDouble("amount"));
		Timestamp ts = null;
		try {
			ts = rs.getTimestamp("created_at");
		} catch (SQLException ignored) {
		}
		if (ts == null) {
			try {
				ts = rs.getTimestamp("bid_time");
			} catch (SQLException ignored) {
			}
		}
		if (ts != null) {
			bid.setBidTime(ts.toLocalDateTime());
		}
		return bid;
	}
}
