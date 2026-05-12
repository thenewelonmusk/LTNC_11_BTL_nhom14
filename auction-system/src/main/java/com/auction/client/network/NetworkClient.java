package com.auction.client.network;

import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

public class NetworkClient {
    private static NetworkClient instance;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Khởi tạo Gson ngay từ đầu để tránh NullPointerException dù kết nối có xịt
    private final Gson gson = new Gson();

    private NetworkClient() {
        connect();
    }

    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        }
        return instance;
    }

    // Tách riêng hàm connect để sau này có thể làm nút "Thử kết nối lại" trên GUI
    public void connect() {
        try {
            socket = new Socket("localhost", 8080);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            System.out.println("Đã kết nối tới Server thành công!");
        } catch (IOException e) {
            System.err.println("LỖI: Không thể kết nối đến Server. Vui lòng bật Server trước!");
            socket = null; // Đảm bảo socket là null nếu lỗi
        }
    }

    public String sendAndReceive(String action, Object dataPayload) {
        // Kiểm tra an toàn trước khi gửi
        if (socket == null || out == null || in == null) {
            System.err.println("Chưa có kết nối tới Server.");
            return null;
        }

        try {
            // 1. Biến Object (LoginRequest, RegisterRequest) thành JSON String
            String jsonData = gson.toJson(dataPayload);

            // 2. Bọc lại thành một khung chuẩn giao tiếp (Wrapper)
            // Ví dụ: {"action": "LOGIN", "data": "{\"username\":\"...\"}"}
            String requestMessage = "{\"action\":\"" + action + "\", \"data\":" + jsonData + "}";

            // 3. Gửi lên Server
            out.println(requestMessage);

            // 4. Chờ Server xử lý và đọc phản hồi
            return in.readLine();

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}