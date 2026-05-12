package com.auction.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static Connection connection = null;

    public static Connection getConnection() {
        try {
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
}