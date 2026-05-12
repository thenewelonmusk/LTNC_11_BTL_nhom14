package com.auction.dao;

import com.auction.dto.ItemRequest;
import com.auction.model.item.Item;
import com.auction.model.item.ItemFactory;

import java.util.ArrayList;
import java.util.List;
import java.sql.*;

public class ItemDAO {
//    public Long saveItem(ItemRequest req) throws Exception {
//        boolean isUpdate = req.getItemId() != null && req.getItemId() > 0;
//        String sql;
//        if (isUpdate) {
//            sql = "UPDATE items SET name = ?, description = ?, category = ?, starting_price = ?, seller_id = ? WHERE id = ?";
//        } else {
//            sql = "INSERT INTO items (name, description, category, starting_price, seller_id) VALUES (?, ?, ?, ?, ?)";
//        }
//
//        // SỬA: Thêm Statement.RETURN_GENERATED_KEYS để lấy ID mới
//        try (Connection conn = DatabaseConnection.getConnection();
//             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
//
//            stmt.setString(1, req.getName());
//            stmt.setString(2, req.getDescription());
//            stmt.setString(3, req.getType());
//            stmt.setDouble(4, req.getStartingPrice());
//            stmt.setLong(5, req.getSellerId());
//
//            if (isUpdate) {
//                stmt.setLong(6, req.getItemId());
//                stmt.executeUpdate();
//                return req.getItemId(); // Trả về chính ID cũ nếu là Update
//            } else {
//                stmt.executeUpdate();
//                // Lấy ID vừa được MySQL sinh ra
//                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
//                    if (generatedKeys.next()) {
//                        return generatedKeys.getLong(1);
//                    }
//                }
//            }
//            throw new Exception("SAVE_FAILED");
//        } catch (SQLException e) {
//            e.printStackTrace();
//            throw new Exception("DATABASE_ERROR");
//        }
//    }

    public boolean createItem(ItemRequest request) throws Exception {
        String sql = "INSERT INTO items (name, description, category, starting_price, seller_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, request.getName());
            stmt.setString(2, request.getDescription());
            stmt.setString(3, request.getType());
            stmt.setDouble(4, request.getStartingPrice());
            stmt.setLong(5, request.getSellerId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace(); // Quan trọng để debug
            throw new Exception("DATABASE_ERROR");
        }
    }

    public boolean updateItem(ItemRequest request) throws Exception {
        String sql = "UPDATE items SET name = ?, description = ?, category = ?, starting_price = ?, seller_id = ? WHERE id = ?";
        // SỬA: Đưa stmt vào try-with-resources
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, request.getName());
            stmt.setString(2, request.getDescription());
            stmt.setString(3, request.getType());
            stmt.setDouble(4, request.getStartingPrice());
            stmt.setLong(5, request.getSellerId());
            stmt.setLong(6, request.getItemId());

            if (stmt.executeUpdate() > 0) return true;
            throw new Exception("SAVE_FAILED");
        } catch (SQLException e) {
            e.printStackTrace(); // SỬA: Thêm printStackTrace
            throw new Exception("DATABASE_ERROR");
        }
    }

    public Item findItem(Long itemId) throws Exception {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, itemId);
            // SỬA: Đưa ResultSet vào try-with-resources
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToItem(rs);
                }
                throw new Exception("ITEM_NOT_FOUND");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("DATABASE_ERROR");
        }
    }

    public boolean deleteItem(Long itemId) throws Exception {
        String sql = "DELETE FROM items WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
           stmt.setLong(1, itemId);

           int rowsAffected = stmt.executeUpdate();
           if (rowsAffected > 0) {
               return true;
           }
           throw new Exception("DELETE_FAILED");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Exception("DATABASE_ERROR");
        }
    }

    public List<Item> findByType(String type) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE category = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, type);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    public List<Item> findBySeller(Long sellerId) {
        List<Item> items = new ArrayList<>();
        String sql = "SELECT * FROM items WHERE seller_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, sellerId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    items.add(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return items;
    }

    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        ItemRequest req = new ItemRequest();
        req.setItemId(rs.getLong("id"));
        req.setName(rs.getString("name"));
        req.setType(rs.getString("category"));
        req.setDescription(rs.getString("description"));
        req.setStartingPrice(rs.getDouble("starting_price"));
        req.setCurrentPrice(rs.getDouble("current_price"));
        req.setSellerId(rs.getLong("seller_id"));
        return ItemFactory.create(req);
    }

    public List<Item> getAllItems() {
        List<Item> items = new ArrayList<>();
        // SỬA: Thêm dấu nháy đơn cho các giá trị ENUM 'OPEN' và 'RUNNING'
        String sql = "SELECT * FROM items WHERE status = 'OPEN' OR status = 'RUNNING'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                items.add(mapResultSetToItem(rs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return items;
    }
}