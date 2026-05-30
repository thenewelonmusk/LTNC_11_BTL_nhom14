package com.auction.dao;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

	public boolean createAuction(Auction a) throws Exception {
		String sql = "INSERT INTO auctions (item_id, seller_id, start_price, current_price, start_time, end_time, status) VALUES (?, ?, ?, ?, ?, ?, ?)";

		if (DatabaseConnection.isInTransaction()) {
			Connection conn = DatabaseConnection.getConnection();
			try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				stmt.setLong(1, a.getItemId());
				stmt.setLong(2, a.getSellerId());
				stmt.setDouble(3, a.getStartingPrice());
				stmt.setDouble(4, a.getCurrentPrice());
				stmt.setTimestamp(5, Timestamp.valueOf(a.getStartTime()));
				stmt.setTimestamp(6, Timestamp.valueOf(a.getEndTime()));
				stmt.setString(7, a.getStatus().name());

				if (stmt.executeUpdate() > 0) {
					try (ResultSet rs = stmt.getGeneratedKeys()) {
						if (rs.next()) {
							a.setId(rs.getLong(1));
						}
					}
					return true;
				}
				throw new Exception("SAVE_FAILED");
			} catch (SQLException e) {
				throw new Exception("DATABASE_ERROR");
			}
		} else {
			try (Connection conn = DatabaseConnection.getConnection();
					PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
				stmt.setLong(1, a.getItemId());
				stmt.setLong(2, a.getSellerId());
				stmt.setDouble(3, a.getStartingPrice());
				stmt.setDouble(4, a.getCurrentPrice());
				stmt.setTimestamp(5, Timestamp.valueOf(a.getStartTime()));
				stmt.setTimestamp(6, Timestamp.valueOf(a.getEndTime()));
				stmt.setString(7, a.getStatus().name());

				if (stmt.executeUpdate() > 0) {
					try (ResultSet rs = stmt.getGeneratedKeys()) {
						if (rs.next()) {
							a.setId(rs.getLong(1));
						}
					}
					return true;
				}
				throw new Exception("SAVE_FAILED");
			} catch (SQLException e) {
				throw new Exception("DATABASE_ERROR");
			}
		}
	}

	public boolean updateAuction(Auction a) throws Exception {
		// FIX (anti-sniping): phải UPDATE cả end_time, nếu không thì việc
		// gia hạn 60 giây trong BidServiceImpl chỉ tồn tại in-memory
		// và bị mất ngay khi findAuction() đọc lại từ DB.
		String sql = "UPDATE auctions SET current_price = ?, winner_id = ?, status = ?, end_time = ? WHERE id = ?";

		if (DatabaseConnection.isInTransaction()) {
			Connection conn = DatabaseConnection.getConnection();
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setDouble(1, a.getCurrentPrice());
				if (a.getWinnerId() != null) {
					stmt.setLong(2, a.getWinnerId());
				} else {
					stmt.setNull(2, Types.BIGINT);
				}
				stmt.setString(3, a.getStatus().name());
				if (a.getEndTime() != null) {
					stmt.setTimestamp(4, Timestamp.valueOf(a.getEndTime()));
				} else {
					stmt.setNull(4, Types.TIMESTAMP);
				}
				stmt.setLong(5, a.getId());

				if (stmt.executeUpdate() > 0) {
					return true;
				}
				throw new Exception("SAVE_FAILED");
			} catch (SQLException e) {
				throw new Exception("DATABASE_ERROR");
			}
		} else {
			try (Connection conn = DatabaseConnection.getConnection();
					PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setDouble(1, a.getCurrentPrice());
				if (a.getWinnerId() != null) {
					stmt.setLong(2, a.getWinnerId());
				} else {
					stmt.setNull(2, Types.BIGINT);
				}
				stmt.setString(3, a.getStatus().name());
				if (a.getEndTime() != null) {
					stmt.setTimestamp(4, Timestamp.valueOf(a.getEndTime()));
				} else {
					stmt.setNull(4, Types.TIMESTAMP);
				}
				stmt.setLong(5, a.getId());

				if (stmt.executeUpdate() > 0) {
					stmt.close();
					return true;
				}
				stmt.close();
				throw new Exception("SAVE_FAILED");
			} catch (SQLException e) {
				throw new Exception("DATABASE_ERROR");
			}
		}
	}

	public Auction findAuction(Long id) throws Exception {
		String sql = "SELECT * FROM auctions WHERE id = ?";

		if (DatabaseConnection.isInTransaction()) {
			Connection conn = DatabaseConnection.getConnection();
			try (PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, id);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return mapRow(rs);
					}
					throw new Exception("AUCTION_NOT_FOUND");
				}
			} catch (SQLException e) {
				if (e.getMessage() != null && e.getMessage().equals("AUCTION_NOT_FOUND")) {
					throw e;
				}
				throw new Exception("DATABASE_ERROR");
			}
		} else {
			try (Connection conn = DatabaseConnection.getConnection();
					PreparedStatement stmt = conn.prepareStatement(sql)) {
				stmt.setLong(1, id);
				try (ResultSet rs = stmt.executeQuery()) {
					if (rs.next()) {
						return mapRow(rs);
					}
					throw new Exception("AUCTION_NOT_FOUND");
				}
			} catch (SQLException e) {
				if (e.getMessage() != null && e.getMessage().equals("AUCTION_NOT_FOUND")) {
					throw e;
				}
				throw new Exception("DATABASE_ERROR");
			}
		}
	}

	public List<Auction> findByItemId(Long itemId) {
		List<Auction> list = new ArrayList<>();
		String sql = "SELECT * FROM auctions WHERE item_id = ?";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, itemId);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next())
					list.add(mapRow(rs));
			}
		} catch (Exception e) {
		}
		return list;
	}

	public List<Auction> findAll() {
		List<Auction> list = new ArrayList<>();
		String sql = "SELECT * FROM auctions";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql);
				ResultSet rs = stmt.executeQuery()) {
			while (rs.next())
				list.add(mapRow(rs));
		} catch (Exception e) {
		}
		return list;
	}

	public List<Auction> findByStatus(AuctionStatus status) {
		List<Auction> list = new ArrayList<>();
		String sql = "SELECT * FROM auctions WHERE status = ?";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setString(1, status.name());
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next())
					list.add(mapRow(rs));
			}
		} catch (Exception e) {
		}
		return list;
	}

	public List<Auction> findBySellerId(Long sellerId) {
		List<Auction> list = new ArrayList<>();
		String sql = "SELECT * FROM auctions WHERE seller_id = ?";
		try (Connection conn = DatabaseConnection.getConnection();
				PreparedStatement stmt = conn.prepareStatement(sql)) {
			stmt.setLong(1, sellerId);
			try (ResultSet rs = stmt.executeQuery()) {
				while (rs.next())
					list.add(mapRow(rs));
			}
		} catch (Exception e) {
		}
		return list;
	}

	private Auction mapRow(ResultSet rs) throws SQLException {
		Auction a = new Auction();
		a.setId(rs.getLong("id"));
		a.setItemId(rs.getLong("item_id"));
		a.setSellerId(rs.getLong("seller_id"));
		a.setStartingPrice(rs.getDouble("start_price"));
		a.setCurrentPrice(rs.getDouble("current_price"));
		long wid = rs.getLong("winner_id");
		if (!rs.wasNull())
			a.setWinnerId(wid);
		a.setStartTime(rs.getTimestamp("start_time").toLocalDateTime());
		a.setEndTime(rs.getTimestamp("end_time").toLocalDateTime());
		a.setStatus(AuctionStatus.valueOf(rs.getString("status")));
		return a;
	}
}