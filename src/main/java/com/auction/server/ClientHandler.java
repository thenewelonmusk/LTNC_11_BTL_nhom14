package com.auction.server;

import com.auction.dto.*;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.service.AuctionService;
import com.auction.service.AutoBidService;
import com.auction.service.BidService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

    // 1. Danh sách lưu trữ tất cả các client đang kết nối để làm Real-time Broadcast [cite: 346]
    public static final List<PrintWriter> activeHandlers = new CopyOnWriteArrayList<>();

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private Gson gson;

    // 2. Các Service dùng chung (Singleton) truyền từ AuctionServer vào [cite: 1011, 1013]
    private AuctionService auctionService;
    private BidService bidService;

    public ClientHandler(Socket socket, AuctionService auctionService, BidService bidService)  {
        this.socket = socket;
        this.auctionService = auctionService;
        this.bidService = bidService;
        this.gson = new Gson();
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Thêm client này vào danh sách phát sóng chung
            activeHandlers.add(out);

            String requestLine;
            while ((requestLine = in.readLine()) != null) {
                // Tách Header (Hành động) và Payload (Dữ liệu JSON)
                String[] parts = requestLine.split("\\|", 2);
                String action = parts[0];
                String dataObj = parts.length > 1 ? parts[1] : "";

                // Phân luồng xử lý các Case trong hệ thống 
                switch (action) {
                    case "PLACE_BID":
                        handlePlaceBid(dataObj);
                        break;
                    case "SETUP_AUTO_BID":
                        handleSetupAutoBid(dataObj);
                        break;
                    case "LOGIN":
                        handleLogin(dataObj);
                        break;
                    case "REGISTER":
                        handleRegister(dataObj);
                        break;
                    case "LIST_MY_ITEMS":
                    case "GET_ALL_AUCTIONS":
                        // Thêm logic gọi AuctionService lấy danh sách ở đây
                        // out.println("AUCTION_LIST|" + gson.toJson(auctionList));
                        break;
                    case "SAVE_ITEM":
                        // Thêm logic gọi hàm lưu Item từ nhóm bạn ở đây
                        break;
                    default:
                        System.out.println("[WARNING] Hành động không xác định từ Client: " + action);
                }
            }
        } catch (Exception e) {
            System.out.println("[INFO] Client ngắt kết nối hoặc có lỗi mạng: " + e.getMessage());
        } finally {
            // 3. Quan trọng: Dọn dẹp bộ nhớ khi Client thoát (Tránh Memory Leak) [cite: 349]
            if (out != null) {
                activeHandlers.remove(out);
            }
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // ====================================================================================
    // KHỐI XỬ LÝ LOGIC (CONTROLLER METHODS)
    // ====================================================================================

    /**
     * Xử lý luồng đặt giá (Tích hợp Hàng đợi Auto-Bid và Anti-sniping) 
     */
    private void handlePlaceBid(String dataObj) {
        try {
            BidRequest bReq = gson.fromJson(dataObj, BidRequest.class);
            List<BidTransaction> pendingAutoBids = new ArrayList<>();

            // 1. Gọi Service để xử lý bid. Nếu bot kích hoạt, nó sẽ ném giao dịch vào pendingAutoBids
            BidResponse bRes = bidService.placeBid(bReq, bReq.getBidderId(), (autoBid, nextPrice) -> {
                pendingAutoBids.add(autoBid);
            });

            // 2. Phản hồi kết quả lại cho chính Client vừa bấm nút
            out.println("PLACE_BID_RESULT|" + gson.toJson(bRes));

            // 3. Nếu đặt giá thất bại (lỗi logic), dừng lại không Broadcast
            if (!bRes.isSuccess() || bRes.getBid() == null) {
                return;
            }

            // 4. Lấy thời gian kết thúc (phòng trường hợp dính Anti-sniping gia hạn) [cite: 988]
            LocalDateTime newEndTime = null;
            try {
                Auction currentAuction = auctionService.getAuctionById(bReq.getAuctionId());
                if (currentAuction != null) {
                    newEndTime = currentAuction.getEndTime();
                }
            } catch (Exception ignored) {}

            // 5. Broadcast giá của NGƯỜI THẬT lên trước
            broadcastBid(bRes.getBid(), false, newEndTime);

            // 6. Xả hàng đợi: Broadcast lần lượt các giá của BOT AUTO-BID (tạo độ trễ UI) [cite: 863]
            for (BidTransaction autoBid : pendingAutoBids) {
                Thread.sleep(200); // Tạo độ trễ 200ms giúp UI nảy số mượt mà
                broadcastBid(autoBid, true, newEndTime);
            }

        } catch (JsonSyntaxException e) {
            out.println("PLACE_BID_RESULT|{\"success\":false,\"message\":\"Dữ liệu gửi lên không hợp lệ.\"}");
        } catch (Exception e) {
            e.printStackTrace();
            out.println("PLACE_BID_RESULT|{\"success\":false,\"message\":\"Lỗi máy chủ nội bộ.\"}");
        }
    }

    /**
     * Xử lý luồng Client bấm nút "Lưu Auto-Bid"
     */
    private void handleSetupAutoBid(String dataObj) {
        try {
            AutoBidRequest req = gson.fromJson(dataObj, AutoBidRequest.class);
            
            // Gọi Service dùng chung (Singleton) để cài đặt Auto-Bid vào RAM [cite: 1014]
            AutoBidResponse res = autoBidService.setupAutoBid(req, req.getBidderId());
            
            out.println("SETUP_AUTO_BID_RESULT|" + gson.toJson(res));
        } catch (Exception e) {
            out.println("SETUP_AUTO_BID_RESULT|{\"success\":false,\"message\":\"Lỗi thiết lập đấu giá tự động.\"}");
        }
    }

    /**
     * Xử lý Đăng nhập 
     */
    private void handleLogin(String dataObj) {
        try {
            // Tùy thuộc vào cấu trúc LoginRequest của nhóm bạn
            // LoginRequest req = gson.fromJson(dataObj, LoginRequest.class);
            // LoginResponse res = userService.login(req);
            // out.println("LOGIN_RESULT|" + gson.toJson(res));
            
            System.out.println("[INFO] Nhận yêu cầu LOGIN: " + dataObj);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Xử lý Đăng ký 
     */
    private void handleRegister(String dataObj) {
         try {
             // RegisterRequest req = gson.fromJson(dataObj, RegisterRequest.class);
             // RegisterResponse res = userService.register(req);
             // out.println("REGISTER_RESULT|" + gson.toJson(res));
             
             System.out.println("[INFO] Nhận yêu cầu REGISTER: " + dataObj);
         } catch (Exception e) {
             e.printStackTrace();
         }
    }

    // ====================================================================================
    // KHỐI HÀM TIỆN ÍCH (HELPER METHODS)
    // ====================================================================================

    /**
     * Hàm phát sóng giao dịch mới tới tất cả các Client đang theo dõi phòng [cite: 983]
     */
    private void broadcastBid(BidTransaction bid, boolean isAuto, LocalDateTime newEndTime) {
        // Gom dữ liệu cập nhật thành một Object hoặc chuỗi JSON
        // Lưu ý: Tùy chỉnh class RealtimeUpdateResponse cho khớp với DTO bên bạn
        String updatePayload = String.format(
                "{\"bidId\":%d, \"auctionId\":%d, \"bidAmount\":%f, \"bidderId\":%d, \"isAuto\":%b, \"newEndTime\":\"%s\"}",
                bid.getId(),
                bid.getAuctionId(),
                bid.getBidAmount(),
                bid.getBidderId(),
                isAuto,
                (newEndTime != null ? newEndTime.toString() : "")
        );

        for (PrintWriter writer : activeHandlers) {
            try {
                writer.println("UPDATE_BID|" + updatePayload);
            } catch (Exception e) {
                System.out.println("[WARNING] Lỗi khi broadcast tới 1 client: " + e.getMessage());
            }
        }
    }
}