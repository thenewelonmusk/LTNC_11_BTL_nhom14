package com.auction.server;

import com.auction.dao.*;
import com.auction.dto.*;
import com.auction.service.*;
import com.auction.service.impl.*;
import com.google.gson.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson;

    private UserService userService;
    private ItemService itemService;
    private AuctionService auctionService;
    private BidService bidService;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;

        // Cấu hình Gson để đọc/ghi thời gian chuẩn xác
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                    @Override
                    public void write(com.google.gson.stream.JsonWriter out, LocalDateTime value) throws IOException {
                        out.value(value != null ? value.toString() : null);
                    }
                    @Override
                    public LocalDateTime read(com.google.gson.stream.JsonReader in) throws IOException {
                        return LocalDateTime.parse(in.nextString());
                    }
                }).create();

        // Khởi tạo trọn bộ DAO và Service
        UserDAO userDAO = new UserDAO();
        ItemDAO itemDAO = new ItemDAO();
        AuctionDAO auctionDAO = new AuctionDAO();
        BidDAO bidDAO = new BidDAO();

        this.userService = new UserServiceImpl(userDAO);
        this.itemService = new ItemServiceImpl(itemDAO);
        this.auctionService = new AuctionServiceImpl(auctionDAO, itemDAO);
        this.bidService = new BidServiceImpl(bidDAO, auctionDAO, this.auctionService);

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
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("[Request từ Client]: " + clientMessage);

                JsonObject requestObj = JsonParser.parseString(clientMessage).getAsJsonObject();
                String action = requestObj.get("action").getAsString();
                JsonObject dataObj = requestObj.getAsJsonObject("data");

                String jsonResponse = "";

                switch (action) {
                    case "LOGIN":
                        LoginRequest loginReq = gson.fromJson(dataObj, LoginRequest.class);
                        LoginResponse loginRes = userService.login(loginReq);
                        jsonResponse = gson.toJson(loginRes);
                        break;

                    case "REGISTER":
                        RegisterRequest regReq = gson.fromJson(dataObj, RegisterRequest.class);
                        RegisterResponse regRes = userService.register(regReq);
                        jsonResponse = gson.toJson(regRes);
                        break;

                    case "SAVE_ITEM":
                        ItemRequest itemReq = gson.fromJson(dataObj, ItemRequest.class);
                        ItemResponse itemRes;
                        if (itemReq.getItemId() != null && itemReq.getItemId() > 0) {
                            itemRes = itemService.updateItem(itemReq.getItemId(), itemReq, itemReq.getSellerId());
                        } else {
                            itemRes = itemService.createItem(itemReq, itemReq.getSellerId());
                        }
                        jsonResponse = gson.toJson(itemRes);
                        break;

                    case "OPEN_AUCTION":
                        AuctionRequest aReq = gson.fromJson(dataObj, AuctionRequest.class);
                        AuctionResponse aRes = auctionService.openAuction(aReq, aReq.getSellerId());
                        jsonResponse = gson.toJson(aRes);
                        break;

                    case "PLACE_BID":
                        BidRequest bReq = gson.fromJson(dataObj, BidRequest.class);
                        BidResponse bRes = bidService.placeBid(bReq, bReq.getUserId());
                        jsonResponse = gson.toJson(bRes);
                        break;

                    default:
                        jsonResponse = "{\"success\":false, \"message\":\"Hành động không hợp lệ!\"}";
                        break;
                }
                out.println(jsonResponse);
            }
        } catch (Exception e) {
            System.out.println("[-] Client đã ngắt kết nối.");
        } finally {
            try { if (clientSocket != null) clientSocket.close(); } catch (IOException e) {}
        }
    }
}