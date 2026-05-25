# Real-time Auction Status Update Implementation Guide

## Tổng Quan
Hệ thống này cập nhật trạng thái phiên đấu giá (RUNNING/FINISHED) real-time trên tất cả clients mà không cần người dùng thực hiện hành động nào (như đặt bid).

---

## 1. Cách Hoạt Động

### Luồng Xử Lý
```
Server Monitor (1 giây)
    ↓
Kiểm tra Auction status (OPEN→RUNNING, RUNNING→FINISHED)
    ↓
Nếu thay đổi, gọi ClientHandler.broadcastStatusUpdate()
    ↓
Broadcast JSON message tới TẤT CẢ clients
    ↓
NetworkClient nhận và gọi onAuctionUpdate() trên tất cả listeners
    ↓
BrowseAuctionsViewController & AuctionDetailViewController cập nhật UI
```

---

## 2. Các Thay Đổi Chi Tiết

### 2.1 Backend - AuctionServer.java

**Kích hoạt monitor:**
```java
// Trong main()
startAuctionStatusMonitor(auctionService);
System.out.println("[✓] Luồng kiểm tra trạng thái auction real-time đã bắt đầu");
```

**Monitor logic:**
```java
private static void startAuctionStatusMonitor(AuctionService auctionService){
    final Map<Long, String> previousStatus = new ConcurrentHashMap<>();
    
    scheduler.scheduleAtFixedRate(() -> {
        // Mỗi 1 giây:
        // 1. Lấy danh sách tất cả auctions
        // 2. Gọi auctionService.refreshStatus() để cập nhật status
        // 3. Nếu status thay đổi, phát broadcast
        // 4. Lưu status cũ để so sánh lần sau
    }, 1, 1, TimeUnit.SECONDS);
}
```

### 2.2 Backend - ClientHandler.java

**Thêm Static Gson Instance:**
```java
private static final Gson staticGson = new GsonBuilder()
    .registerTypeAdapter(LocalDateTime.class, ...)
    .create();
```

**Phương thức broadcast status:**
```java
public static void broadcastStatusUpdate(Long auctionId, String newStatus, LocalDateTime endTime) {
    // Tạo JSON packet:
    // {
    //   "action": "AUCTION_UPDATE",
    //   "data": {
    //     "auctionId": 1,
    //     "status": "FINISHED",
    //     "endTime": "2024-01-15T18:30:00",
    //     "statusChangeTime": "2024-01-15T18:30:01",
    //     "message": "Trạng thái phiên đấu giá được cập nhật: FINISHED"
    //   }
    // }
    
    // Phát tới tất cả activeHandlers
    broadcastUpdate(staticGson.toJson(broadcastObj));
}
```

### 2.3 Client - NetworkClient.java

**Xử lý AUCTION_UPDATE messages:**
```java
private void startListening() {
    new Thread(() -> {
        while ((line = in.readLine()) != null) {
            JsonObject json = JsonParser.parseString(line).getAsJsonObject();
            
            // Kiểm tra action
            if ("AUCTION_UPDATE".equals(json.get("action").getAsString())) {
                JsonObject data = json.getAsJsonObject("data");
                
                // Thông báo cho tất cả listeners
                for (AuctionUpdateListener listener : listeners) {
                    listener.onAuctionUpdate(data);
                }
            }
        }
    }).start();
}
```

### 2.4 Client - BrowseAuctionsViewController.java

**Implement listener:**
```java
public class BrowseAuctionsViewController implements NetworkClient.AuctionUpdateListener {
    
    @Override
    public void initialize() {
        // ...existing code...
        
        // Đăng ký nhận cập nhật real-time
        NetworkClient.getInstance().addListener(this);
    }
    
    @Override
    public void onAuctionUpdate(JsonObject data) {
        Platform.runLater(() -> {
            Long auctionId = data.get("auctionId").getAsLong();
            String newStatus = data.get("status").getAsString();
            
            // Tìm row trong bảng và cập nhật
            for (AuctionRow row : this.data) {
                if (row.getId().equals(auctionId)) {
                    row.status.set(newStatus);
                    row.endTime.set(data.get("endTime").getAsString());
                    break;
                }
            }
        });
    }
}
```

### 2.5 Client - AuctionDetailViewController.java

**Enhanced status handling:**
```java
@Override
public void onAuctionUpdate(JsonObject data) {
    // ... existing bid handling code ...
    
    // Xử lý status updates
    else if (newStatus != null && !newStatus.isEmpty()) {
        applyStatusStyle(newStatus);
        
        if ("FINISHED".equals(newStatus.toUpperCase())) {
            lblBidResult.setText("⏹️ Phiên đấu giá đã kết thúc!");
            btnPlaceBid.setDisable(true);  // ← Không cho phép đặt bid nữa
        } else if ("RUNNING".equals(newStatus.toUpperCase())) {
            lblBidResult.setText("🎯 Phiên đấu giá đang diễn ra...");
            btnPlaceBid.setDisable(false);
        }
    }
}
```

---

## 3. Kiểm Thử

### Test Case 1: Auction OPEN → RUNNING
1. Tạo auction với startTime trong tương lai
2. Đợi server monitor chạy (1 giây)
3. Khi thời gian bắt đầu đến:
   - ✅ Bảng duyệt auctions: status tự động → RUNNING
   - ✅ Chi tiết phiên: status tự động → RUNNING
   - ✅ Nút đặt bid: còn cho phép click

### Test Case 2: Auction RUNNING → FINISHED
1. Mở chi tiết phiên đấu giá đang chạy
2. Đợi thời gian kết thúc đến:
   - ✅ Status tự động → FINISHED
   - ✅ Nút "Đặt giá" tự động bị vô hiệu hóa
   - ✅ Hiển thị thông báo "Phiên đấu giá đã kết thúc"
3. Quay lại bảng duyệt:
   - ✅ Status trong bảng cũng được cập nhật → FINISHED

### Test Case 3: Multiple Clients
1. Mở ứng dụng trên 2 clients cùng xem phiên
2. Thời gian kết thúc:
   - ✅ Cả 2 clients nhận cập nhật status
   - ✅ Cả 2 clients không cho phép đặt bid

---

## 4. Lợi Ích

✅ **Tự động cập nhật**: Không cần refresh hay tương tác
✅ **Real-time**: Cập nhật mỗi 1 giây trên server
✅ **Toàn hệ thống**: Broadcast tới tất cả clients
✅ **Tránh lỗi Logic**: Không cho phép bid trên auction đã FINISHED
✅ **Trải nghiệm tốt**: User thấy ngay trạng thái thay đổi

---

## 5. Các Thay Đổi File

| File | Thay Đổi |
|------|----------|
| `AuctionServer.java` | Kích hoạt monitor, cải thiện logic |
| `ClientHandler.java` | Thêm broadcastStatusUpdate(), staticGson |
| `BrowseAuctionsViewController.java` | Implement listener, onAuctionUpdate() |
| `AuctionDetailViewController.java` | Cải thiện onAuctionUpdate() xử lý status |
| `NetworkClient.java` | Không thay đổi (đã có listener pattern) |

---

## 6. Troubleshooting

**Q: Status không cập nhật?**
- A: Kiểm tra AuctionServer có in "[✓] Luồng kiểm tra..." khi khởi động
- A: Kiểm tra console có lỗi gì trong `ClientHandler.broadcastStatusUpdate()`

**Q: Nút đặt bid không bị vô hiệu khi FINISHED?**
- A: Kiểm tra `AuctionDetailViewController.onAuctionUpdate()` có xử lý status hay không
- A: Kiểm tra `btnPlaceBid.setDisable(true)` được gọi

**Q: Chỉ một client nhận cập nhật?**
- A: Kiểm tra `NetworkClient.addListener()` được gọi trong initialize()
- A: Kiểm tra CopyOnWriteArrayList `listeners` trong NetworkClient

---

## 7. Mở Rộng Tương Lai

- Thêm thông báo popup khi status thay đổi
- Lưu history các lần status thay đổi
- Integrate với sound notification
- Cấp độ log chi tiết hơn
