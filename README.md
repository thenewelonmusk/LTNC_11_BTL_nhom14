# 🏛 Hệ thống Đấu giá Trực tuyến – Nhóm 14

> **Môn:** Lập trình nâng cao  
> **Đề tài:** Hệ thống đấu giá trực tuyến (Online Auction System)  
> **Kiến trúc:** JavaFX (client) ↔ Socket TCP ↔ Java Server ↔ MySQL

---

## 📋 Mục lục

1. [Yêu cầu hệ thống](#1-yêu-cầu-hệ-thống)
2. [Cài đặt MySQL Database](#2-cài-đặt-mysql-database)
3. [Cấu hình kết nối](#3-cấu-hình-kết-nối-trong-code)
4. [Build & chạy dự án](#4-build--chạy-dự-án)
5. [Tài khoản mẫu](#5-tài-khoản-mẫu)
6. [Cấu trúc bảng](#6-cấu-trúc-bảng)
7. [Cấu trúc thư mục](#7-cấu-trúc-thư-mục-dự-án)
8. [Tính năng chính](#8-tính-năng-chính)
9. [Xử lý sự cố](#9-xử-lý-sự-cố-troubleshooting)

---

## 1. Yêu cầu hệ thống

| Thành phần | Phiên bản tối thiểu |
|---|---|
| **JDK** | Java 17 (khuyến nghị 21) |
| **Maven** | 3.6+ |
| **MySQL Server** | 8.0+ |
| **JavaFX** | 21 (đã khai báo trong `pom.xml`) |
| **IDE** | IntelliJ IDEA / Eclipse / NetBeans |
| **OS** | Windows 10/11, macOS, Linux |

---

## 2. Cài đặt MySQL Database

### Bước 1 – Cài MySQL Server

- **Windows:** Tải MySQL Installer từ <https://dev.mysql.com/downloads/installer/>, chọn `MySQL Server 8.0` + `MySQL Workbench`.
- **macOS:** `brew install mysql` rồi `brew services start mysql`
- **Linux (Ubuntu):** `sudo apt install mysql-server` rồi `sudo systemctl start mysql`

Trong quá trình cài, đặt mật khẩu cho user `root` (mặc định trong code là `password`, xem mục 3 nếu cần đổi).

### Bước 2 – Chạy script tạo database

Mở terminal/CMD ở thư mục gốc dự án rồi chạy một trong các cách sau:

**Cách 1 – Dòng lệnh (nhanh nhất):**

```bash
mysql -u root -p < database/schema.sql
```

Nhập mật khẩu MySQL khi được yêu cầu.

**Cách 2 – MySQL Workbench:**

1. Mở MySQL Workbench, kết nối tới `localhost:3306`
2. `File → Open SQL Script…` chọn `database/schema.sql`
3. Nhấn `⚡ Execute` (Ctrl+Shift+Enter)

**Cách 3 – CMD/PowerShell trên Windows:**

```cmd
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p auction_db < database\schema.sql
```

### Bước 3 – Kiểm tra

```sql
USE auction_db;
SHOW TABLES;
-- Phải thấy: users, items, auctions, bids

SELECT username, role FROM users;
-- Phải có 6 dòng tài khoản mẫu
```

---

## 3. Cấu hình kết nối trong code

Mở file: `src/main/java/com/auction/dao/DatabaseConnection.java`

```java
String url  = "jdbc:mysql://localhost:3306/auction_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
String user = "root";
String pass = "password";   // ⚠️ Sửa thành mật khẩu MySQL của bạn
```

> 💡 **Lưu ý:** Nếu MySQL của bạn dùng port khác `3306` hoặc user khác `root`, sửa luôn ở dòng `url` và `user`.

---

## 4. Build & chạy dự án

### 4.1. Build bằng Maven

```bash
mvn clean compile
```

Lần đầu Maven sẽ tải các thư viện (JavaFX 21, Gson, MySQL Connector 8.0.33…) — chờ 1-2 phút.

### 4.2. Chạy Server (bắt buộc trước)

```bash
mvn exec:java -Dexec.mainClass="com.auction.server.AuctionServer"
```

Hoặc trong IntelliJ: chuột phải `AuctionServer.java` → `Run 'AuctionServer.main()'`.

Khi server chạy thành công sẽ thấy log đại loại:

```
[Server] Đang lắng nghe trên port 8080...
```

### 4.3. Chạy Client (JavaFX UI)

Mở **terminal khác**, chạy:

```bash
mvn javafx:run
```

Hoặc trong IntelliJ: chạy `com.auction.client.MainApp` (hoặc `Launcher`).

> 💡 **Mẹo demo realtime:** chạy **2 client cùng lúc**, đăng nhập bằng 2 tài khoản BIDDER khác nhau (vd `bidder1` và `bidder2`), cùng vào 1 phiên đấu giá. Khi 1 bên đặt giá → bên kia thấy biểu đồ tự cập nhật ngay tức thì.

---

## 5. Tài khoản mẫu

Script `schema.sql` đã tạo sẵn các tài khoản sau:

| Username  | Password    | Role    | Ghi chú |
|-----------|-------------|---------|---------|
| `admin`   | `admin123`  | ADMIN   | Quản trị |
| `seller1` | `seller123` | SELLER  | Đã có items + auctions mẫu |
| `seller2` | `seller123` | SELLER  | Đã có items + auctions mẫu |
| `bidder1` | `bidder123` | BIDDER  | Đã có lịch sử bid mẫu |
| `bidder2` | `bidder123` | BIDDER  | Đã có lịch sử bid mẫu |
| `bidder3` | `bidder123` | BIDDER  | Đã có lịch sử bid mẫu |

> ⚠️ Mật khẩu lưu **plain-text** trong DB để khớp với code `UserDAO.java` hiện tại. Trong môi trường production cần băm bằng BCrypt/Argon2.

---

## 6. Cấu trúc bảng

```
┌──────────┐         ┌──────────┐         ┌──────────────┐
│  users   │────────▶│  items   │────────▶│   auctions   │
│  (id)    │ seller  │ (id,     │ item_id │  (id,        │
│          │         │  seller) │         │   winner_id) │
└──────────┘         └──────────┘         └──────┬───────┘
                                                  │ auction_id
                                                  ▼
                                          ┌──────────────┐
                                          │     bids     │
                                          └──────────────┘
```

| Bảng | Mô tả |
|---|---|
| **users**     | Tài khoản người dùng (BIDDER / SELLER / ADMIN) |
| **items**     | Sản phẩm do seller đăng |
| **auctions**  | Phiên đấu giá gắn với 1 sản phẩm |
| **bids**      | Lịch sử đặt giá — nguồn dữ liệu cho biểu đồ realtime |

Tất cả khóa ngoại đều có `ON DELETE CASCADE` (trừ `winner_id` dùng `SET NULL`), nên khi xóa user/item sẽ tự dọn dữ liệu phụ thuộc.

---

## 7. Cấu trúc thư mục dự án

```
auctionproj_fixed/
├── database/
│   └── schema.sql                      ⭐ Script tạo DB
├── pom.xml                              # Cấu hình Maven
├── README.md                            # File này
└── src/main/
    ├── java/com/auction/
    │   ├── client/                      # JavaFX UI (controllers, views)
    │   │   ├── controller/
    │   │   │   └── AuctionDetailViewController.java  ⭐ Có chart realtime
    │   │   ├── network/NetworkClient.java
    │   │   ├── MainApp.java
    │   │   └── Session.java
    │   ├── server/                      # TCP server + handler
    │   │   ├── AuctionServer.java       ⭐ Main entry của Server
    │   │   └── ClientHandler.java
    │   ├── dao/                         # JDBC Data Access
    │   │   ├── DatabaseConnection.java  ⭐ Sửa user/pass ở đây
    │   │   ├── UserDAO.java
    │   │   ├── ItemDAO.java
    │   │   ├── AuctionDAO.java
    │   │   └── BidDAO.java
    │   ├── service/                     # Business logic
    │   ├── model/                       # Entity classes
    │   └── dto/                         # Request/Response objects
    └── resources/
        ├── fxml/views/                  # FXML giao diện
        │   └── AuctionDetailView.fxml   ⭐ View có LineChart
        └── css/styles.css
```

---

## 8. Tính năng chính

### Cho BIDDER
- 🔍 Duyệt danh sách phiên đấu giá
- 🎯 Vào chi tiết phiên, đặt giá thầu thủ công
- 💰 Quick-bid: nút `+10.000` / `+50.000` / `+100.000`
- 📈 **Biểu đồ giá realtime** — tự động vẽ điểm mới khi có bid (KHÔNG cần refresh)
- ⏰ Đồng hồ đếm ngược thời gian còn lại (đỏ cảnh báo dưới 60s)
- 👑 Highlight bidder cao nhất + tô màu các bid của chính mình
- 📜 Lịch sử bid của bản thân

### Cho SELLER
- ➕ Tạo / sửa / xóa sản phẩm (Vehicle, Electronics, Art)
- 🚀 Mở phiên đấu giá cho sản phẩm
- 🛎 Quản lý các phiên đã mở
- 📦 Quản lý kho sản phẩm

### Cơ chế Realtime
Server broadcast gói `AUCTION_UPDATE` qua TCP tới tất cả client đang xem cùng phiên. Client dùng pattern **Observer** (`NetworkClient.AuctionUpdateListener`) để các view tự cập nhật UI mà không cần polling.

---

## 9. Xử lý sự cố (Troubleshooting)

### ❌ `Communications link failure` / `Access denied for user 'root'@'localhost'`
→ Sai mật khẩu MySQL. Sửa trong `DatabaseConnection.java`.

### ❌ `Unknown database 'auction_db'`
→ Chưa chạy `schema.sql`. Quay lại **Bước 2**.

### ❌ `Public Key Retrieval is not allowed`
→ Đã có `allowPublicKeyRetrieval=true` trong URL JDBC, không cần làm gì. Nếu vẫn báo lỗi, kiểm tra MySQL Connector version (cần 8.0+).

### ❌ Server bind port 8080 fail (`Address already in use`)
→ Có process khác đang chiếm port 8080. Tắt nó hoặc đổi port trong `AuctionServer.java` và `NetworkClient.java`.

### ❌ JavaFX không chạy: `Error: JavaFX runtime components are missing`
→ Dùng `mvn javafx:run` thay vì chạy trực tiếp `java`. Hoặc cài JavaFX SDK 21 và thêm `--module-path` thủ công.

### ❌ Biểu đồ realtime không cập nhật
- Kiểm tra Server đang chạy (port 8080)
- Kiểm tra log Server xem `AUCTION_UPDATE` có được broadcast không
- Tab Console của client xem có `[Network] Kết nối đến Server thành công!` không

### 🔄 Reset toàn bộ DB về trạng thái ban đầu
```bash
mysql -u root -p < database/schema.sql
```
Script có `DROP DATABASE IF EXISTS auction_db` ở đầu nên sẽ tự xóa & tạo lại.

---

## 📝 Ghi chú phát triển

- **Encoding:** UTF-8 (utf8mb4) — hỗ trợ tiếng Việt + emoji
- **Timezone:** UTC (cấu hình trong JDBC URL)
- **Concurrency:** Server xử lý mỗi client bằng 1 thread (`ClientHandler`). Bid được serialize qua `synchronized` để tránh race condition.
- **Format giá:** VND, hiển thị `1,000,000 ₫` ở UI; lưu DB dạng `DECIMAL(18,2)`.

---

**👥 Nhóm 14 – LTNC 2025**
