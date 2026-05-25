# Fix realtime auto-bid + anti-sniping (cập nhật 25/05/2026)

Mô tả ngắn các thay đổi để vá 3 lỗi giao diện:
1. Auto-bid chạy ngầm trên server nhưng không hiện trên client khác.
2. Bid mới (đặc biệt là auto-bid) không lên chart realtime.
3. Có thông báo "đã gia hạn" nhưng phiên vẫn kết thúc đúng giờ cũ.

---

## File đã sửa

### 1. `dao/AuctionDAO.java` — anti-sniping được PERSIST
Lệnh `UPDATE auctions ... WHERE id=?` trước đây thiếu cột `end_time`. Khi `BidServiceImpl` gọi `auction.setEndTime(... +60s)` rồi save, giá trị mới chỉ tồn tại in-memory và bị xoá ở lần `findAuction` kế tiếp. **Đã thêm `end_time = ?` vào câu UPDATE**.

### 2. `dao/BidDAO.java` — lưu/đọc `auto_bid`, sort theo thời gian
- INSERT có thêm cột `auto_bid` (schema đã có sẵn).
- `mapRow` đọc lại cờ `auto_bid` từ DB.
- `findByAuctionId` đổi từ `ORDER BY amount DESC` sang `ORDER BY id ASC` để chart vẽ đúng tiến trình thời gian. Bảng bid vẫn sort theo amount ở phía client.

### 3. `dao/UserDAO.java` — thêm `findUsernameById`
Để server broadcast hiển thị tên thật ("nguyenA") thay vì "User #5".

### 4. `service/AutoBidService.java`, `service/BidService.java` — thêm overload có Listener
Thêm interface `AutoBidListener` để mỗi auto-bid được đặt sẽ gọi callback ngay → server broadcast realtime từng bước nhảy giá. Thêm:
- `BidService.placeBid(request, bidderId, listener)`
- `BidService.registerAutoBidWithLock(request, listener)`
- `AutoBidService.triggerAutoBids(auctionId, listener)`
- `AutoBidService.registerAutoBidAndTrigger(request, listener)`

Các phương thức cũ vẫn còn (backward compatible) — test cũ chạy bình thường.

### 5. `service/impl/BidServiceImpl.java`
- Constructor 4 tham số share **cùng `ConcurrentHashMap<Long, ReentrantLock>`** với `AutoBidServiceImpl` qua `setSharedLockMap()` → manual bid và auto-bid không bao giờ chạy đồng thời trên cùng phiên.
- `placeBid` truyền listener xuống `triggerAutoBids` → mỗi auto-bid được phát ra ngay (server sẽ broadcast).
- Anti-sniping được sửa logic: `newEnd = max(oldEnd + 60s, now + 60s)`. Bid ở giây thứ 0 cũng được gia hạn đủ 60s — không còn việc gia hạn vô nghĩa nếu `oldEnd` đã trôi qua `now`.

### 6. `service/impl/AutoBidServiceImpl.java`
- Dùng chung lock với `BidServiceImpl` (set qua `setSharedLockMap`).
- Thêm `triggerAutoBids(auctionId, listener)` và `registerAutoBidAndTrigger(req, listener)` — listener nhận từng auto-bid được đặt thành công.
- Auto-bid cũng tham gia anti-sniping: nếu nhảy giá trong 30s cuối, kéo dài phiên thêm 60s và `updateAuction` (đã persist `end_time`) → mọi client đều thấy phiên còn sống.

### 7. `server/ClientHandler.java`
- **Sửa bug nghiêm trọng nhất**: chuyển sang dùng constructor **4 tham số** của `BidServiceImpl(bidDAO, auctionDAO, auctionService, autoBidService)`. Trước đây dùng constructor 3 tham số → `autoBidService=null` → `placeBid` không bao giờ kích hoạt auto-bid. Đây là lý do auto-bid trông như chưa cài.
- Tách hàm `broadcastBid(auctionId, bid, isAuto, message)` dùng lại cho 3 nơi:
  - bid manual vừa thành công,
  - mỗi auto-bid trong cascade sau bid manual,
  - mỗi auto-bid trong cascade sau khi đăng ký auto-bid.
- Broadcast carries: `bidId`, `bidderId`, `bidderName` (lookup từ DB + cache), `amount` (đặt thành `currentPrice`), `bidTime`, `autoBid` (true/false), `endTime` mới (đọc lại từ DB sau khi vừa persist), `status`.
- `REGISTER_AUTO_BID` đi qua `bidService.registerAutoBidWithLock(...)` → nắm lock chung và kích hoạt cascade ngay khi đăng ký. Nếu maxBid cao hơn người dẫn đầu, các client khác thấy auto-bid nhảy giá NGAY trong vài ms sau khi nhấn nút.
- `synchronized(out)` quanh mọi `out.println(...)` để broadcast và response không bao giờ xen kẽ trên cùng socket → tránh JSON gãy.
- `GET_AUCTION_DETAIL` và `LIST_MY_BIDS` trả thêm `autoBid` và `bidderName` thật.

### 8. `client/controller/AuctionDetailViewController.java`
- **Fix chart**: `appendChartPoint` đổi từ "số giây kể từ lúc mở view" sang **chỉ số bid** (`priceSeries.getData().size()`). Trùng đơn vị với `buildInitialChart` (FXML cũng đã ghi label trục X là "Lượt bid"). Trước đây 2 hàm dùng đơn vị khác nhau → đường biểu đồ nhảy lung tung.
- Đọc cờ `autoBid` cả từ historical (`GET_AUCTION_DETAIL.bids[].autoBid`) lẫn realtime (`AUCTION_UPDATE.data.autoBid`).
- `BidRow` có thêm cờ `autoBid` — auto-bid được gắn nhãn `🤖 username [AUTO]` trong bảng.
- `onAuctionUpdate` chỉ `setupCountdown` khi `endTime` thay đổi thật (tránh restart Timeline mỗi tick).
- Bỏ `bidCount++` thừa trong `onAuctionUpdate` (đã có trong `appendChartPoint`).
- Lable kết quả phân biệt 🤖 (auto) vs 🔔 (manual).

---

## Kiểm thử

1. Chạy lại schema SQL — schema cũ vẫn chạy được, không cần migration.
2. Mở 2 client cùng xem 1 phiên đấu giá:
   - **Bid thường**: client A đặt bid → client B thấy hàng mới + điểm chart mới trong < 1 giây.
   - **Auto-bid**: client B đăng ký auto-bid `maxBid=30,000,000 / increment=100,000`. Client A đặt bid nhỏ → cả 2 client cùng lúc thấy 1 bid manual + một hoặc nhiều dòng 🤖 [AUTO] xuất hiện liên tiếp, chart nhảy thành chuỗi bậc thang.
   - **Anti-sniping**: chờ countdown < 30s rồi đặt bid → cả 2 client cùng thấy countdown nhảy lên thêm ~60s, label `endTime` cập nhật. Reload phiên (GET_AUCTION_DETAIL) → `end_time` đã được lưu xuống DB.
