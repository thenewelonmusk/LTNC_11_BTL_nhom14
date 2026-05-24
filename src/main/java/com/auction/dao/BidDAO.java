package com.auction.dao;

import com.auction.model.BidTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

	public boolean saveBid(BidTransaction bid) throws Exception {
		// FIX: schema có cột auto_bid -> phải lưu để client phân biệt được
		// bid thường vs auto-bid khi load lịch sử.
		// Nếu DB cũ không có cột auto_bid, sẽ fallback sang INSERT mà không có cột này
		String sqlWithAutoBid = "INSERT INTO bids (auction_id, bidder_id, amount, auto_bid) VALUES (?, ?, ?, ?)";
		String sqlWithoutAutoBid = "INSERT INTO bids (auction_id, bidder_id, amount) VALUES (?, ?, ?)";

		try {
			Connection conn = DatabaseConnection.getConnection();
			PreparedStatement stmt = null;

			try {
				// Try with auto_bid column first
				stmt = conn.prepareStatement(sqlWithAutoBid, Statement.RETURN_GENERATED_KEYS);
				stmt.setLong(1, bid.getAuctionId());
				stmt.setLong(2, bid.getBidderId());
				stmt.setDouble(3, bid.getAmount());
				stmt.setInt(4, bid.isAutoBid() ? 1 : 0);

				int affectedRows = stmt.executeUpdate();
				if (affectedRows > 0) {
					try (ResultSet rs = stmt.getGeneratedKeys()) {
						if (rs.next()) {
							bid.setId(rs.getLong(1));
						}
					}
					stmt.close();
					return true;
				}
				stmt.close();
				throw new Exception("SAVE_FAILED");

			} catch (SQLException e) {
				// Check if error is "Unknown column 'auto_bid'"
				if (e.getMessage() != null && e.getMessage().contains("Unknown column")
						&& e.getMessage().contains("auto_bid")) {
					// Fallback: try without auto_bid column
					System.out.println("[BidDAO] auto_bid column not found, using fallback insert");
					stmt = conn.prepareStatement(sqlWithoutAutoBid, Statement.RETURN_GENERATED_KEYS);
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
						stmt.close();
						return true;
					}
					stmt.close();
					throw new Exception("SAVE_FAILED");
				} else {
					// Other SQL errors
					e.printStackTrace();
					throw new Exception("DATABASE_ERROR");
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new Exception("DATABASE_ERROR");
		}
	}

	public List<BidTransaction> findByAuctionId(Long auctionId) {
		List<BidTransaction> list = new ArrayList<>();
		// FIX: trả về theo thứ tự thời gian (id ASC) để chart vẽ đúng tiến trình.
		// Việc sort theo "bid cao nhất ở đầu" để hiển thị bảng được làm ở phía client.
		String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY id ASC";

		try {
			Connection conn = DatabaseConnection.getConnection();
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setLong(1, auctionId);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				list.add(mapRow(rs));
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public List<BidTransaction> findByBidderId(Long bidderId) {
		List<BidTransaction> list = new ArrayList<>();
		String sql = "SELECT * FROM bids WHERE bidder_id = ? ORDER BY id DESC";

		try {
			Connection conn = DatabaseConnection.getConnection();
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setLong(1, bidderId);
			ResultSet rs = stmt.executeQuery();
			while (rs.next()) {
				list.add(mapRow(rs));
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}

	public BidTransaction findHighestBidByAuction(Long auctionId) {
		String sql = "SELECT * FROM bids WHERE auction_id = ? ORDER BY amount DESC LIMIT 1";

		try {
			Connection conn = DatabaseConnection.getConnection();
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setLong(1, auctionId);
			ResultSet rs = stmt.executeQuery();
			if (rs.next()) {
				BidTransaction result = mapRow(rs);
				rs.close();
				stmt.close();
				return result;
			}
			rs.close();
			stmt.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private BidTransaction mapRow(ResultSet rs) throws SQLException {
		BidTransaction bid = new BidTransaction();
		bid.setId(rs.getLong("id"));
		bid.setAuctionId(rs.getLong("auction_id"));
		bid.setBidderId(rs.getLong("bidder_id"));
		bid.setAmount(rs.getDouble("amount"));
		try {
			bid.setAutoBid(rs.getInt("auto_bid") == 1);
		} catch (SQLException ignored) {
		}
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