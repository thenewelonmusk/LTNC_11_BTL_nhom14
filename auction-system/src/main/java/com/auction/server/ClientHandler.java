package com.auction.server;

import com.auction.dao.ItemDAO;
import com.auction.dao.UserDAO;
import com.auction.dto.*;
import com.auction.service.ItemService;
import com.auction.service.UserService;
import com.auction.service.impl.ItemServiceImpl;
import com.auction.service.impl.UserServiceImpl;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson;

    // Khởi tạo Service chuẩn chỉ dùng Database
    private UserService userService;
    private ItemService itemService = new ItemServiceImpl(new ItemDAO());

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
        this.gson = new Gson();
        this.userService = new UserServiceImpl(new UserDAO());

        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            String clientMessage;
            // Liên tục đọc request từ Client này
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("[Request từ Client]: " + clientMessage);

                // 1. Phân tích chuỗi JSON bọc ngoài
                JsonObject requestObj = JsonParser.parseString(clientMessage).getAsJsonObject();
                String action = requestObj.get("action").getAsString();
                JsonObject dataObj = requestObj.getAsJsonObject("data");

                String jsonResponse = "";

                // 2. Điều hướng (Routing) dựa trên Action
                switch (action) {
                    case "LOGIN":
                        // Ép kiểu JSON data thành LoginRequest object
                        LoginRequest loginReq = gson.fromJson(dataObj, LoginRequest.class);
                        // Gọi UserService đã xử lý chuẩn DB
                        LoginResponse loginRes = userService.login(loginReq);
                        // Đóng gói trả lại
                        jsonResponse = gson.toJson(loginRes);
                        break;

                    case "REGISTER":
                        RegisterRequest regReq = gson.fromJson(dataObj, RegisterRequest.class);
                        RegisterResponse regRes = userService.register(regReq);
                        jsonResponse = gson.toJson(regRes);
                        break;

//                    case "SAVE_ITEM":
//                        ItemRequest itemReq = gson.fromJson(dataObj, ItemRequest.class);
//                        ItemResponse itemRes = itemService.saveItem(itemReq, itemReq.getSellerId());
//                        out.println(gson.toJson(itemRes));
//                        break;
                    // TODO sau này: case "GET_ALL_ITEMS", "PLACE_BID", v.v...

                    default:
                        jsonResponse = "{\"success\":false, \"message\":\"Hành động không hợp lệ!\"}";
                        break;
                }

                // 3. Gửi kết quả lại cho NetworkClient của JavaFX
                out.println(jsonResponse);
            }
        } catch (Exception e) {
            System.out.println("[-] Client đã ngắt kết nối: " + clientSocket.getInetAddress());
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}