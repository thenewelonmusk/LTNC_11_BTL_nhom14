# Hệ Thống Đấu Giá Trực Tuyến (Online Auction System)
**Bài Tập Lớn Môn Lập Trình Nâng Cao (LTNC) - Học Kỳ II, 2025–2026**

## 1. Mô tả bài toán và phạm vi hệ thống
Hệ thống Đấu giá trực tuyến là một ứng dụng phần mềm được xây dựng theo kiến trúc **Client-Server**, cho phép nhiều người dùng kết nối cùng lúc để tham gia các phiên đấu giá theo thời gian thực. Hệ thống sử dụng mô hình **MVC (Model-View-Controller)** ở phía Client và chia lớp (Controller - Service - DAO) ở phía Server để tối ưu hoá việc quản lý mã nguồn.

**Phạm vi người dùng:**
- **Seller (Người bán):** Có quyền đăng bán tài sản, quản lý các phiên đấu giá của mình.
- **Bidder (Người mua):** Tham gia theo dõi, cạnh tranh đặt giá trực tiếp trên giao diện và xem lịch sử giá.

## 2. Công nghệ sử dụng và Môi trường chạy
Hệ thống được phát triển thuần bằng công nghệ Java hệ sinh thái tiêu chuẩn.

- **Ngôn ngữ lập trình:** Java (Yêu cầu JDK 17 trở lên).
- **Giao diện (GUI):** JavaFX, FXML, CSS.
- **Mạng & Giao tiếp:** Java Socket thuần (TCP/IP), JSON (thông qua thư viện `Gson`).
- **Cơ sở dữ liệu:** MySQL (Giao tiếp qua JDBC, sử dụng `HikariCP` cho Connection Pooling).
- **Công cụ quản lý & Build:** Maven.
- **Kiểm thử (Testing):** JUnit 5, Mockito.
- **CI/CD:** GitHub Actions (Tự động chạy Unit Test khi push code).

**Yêu cầu cài đặt (Prerequisites):**
- Máy tính phải cài đặt sẵn **JDK 17+** và **Maven**.
- Cài đặt **MySQL Server** và khởi chạy service database.

## 3. Cấu trúc thư mục dự án
Dự án được tổ chức theo chuẩn cấu trúc của Maven:

```text
LTNC_11_BTL_nhom14/
├── database/            # Chứa file script tạo CSDL (schema.sql)
├── src/
│   ├── main/
│   │   ├── java/com/auction/
│   │   │   ├── client/  # Chứa GUI (JavaFX), Controllers và Socket Client
│   │   │   ├── server/  # Socket Server (AuctionServer, ClientHandler dùng Thread Pool)
│   │   │   ├── dao/     # Tầng Data Access Object giao tiếp với MySQL
│   │   │   ├── dto/     # Tầng Data Transfer Object (Đóng gói Request/Response)
│   │   │   ├── model/   # Các Entity tĩnh (User, Item, Auction, BidTransaction)
│   │   │   └── service/ # Tầng Business Logic trung tâm
│   │   └── resources/   # Chứa các file giao diện (.fxml) và style (.css)
│   └── test/            # Thư mục chứa các Unit Test (Mockito) cho Service, DAO, Network
├── pom.xml              # Tệp tin cấu hình các Dependencies của Maven
└── README.md            # Tài liệu dự án

```

## 4. Vị trí các file đóng gói (.jar)

Sau khi thực hiện lệnh build Maven (`mvn clean package`), hệ thống sẽ tạo ra các tệp tin thực thi động độc lập tại thư mục `target/`.

* **Server Executable:** `target/server.jar` (hoặc tên tương ứng định nghĩa trong `maven-shade-plugin`).
* **Client Executable:** `target/client.jar`.

*(Lưu ý: Các file này là Fat/Uber JAR, đã được đóng gói kèm toàn bộ thư viện như Gson, MySQL Connector, JavaFX).*

## 5. Hướng dẫn cài đặt và khởi chạy

Vui lòng làm theo trình tự dưới đây để khởi chạy hệ thống tránh lỗi mất kết nối (Connection Refused).

### Bước 1: Khởi tạo Cơ sở dữ liệu

1. Mở MySQL Workbench hoặc Terminal.
2. Import file `database/schema.sql` để tạo database `auction_system` và các bảng dữ liệu.
3. Cập nhật thông tin tài khoản (Username/Password) truy cập CSDL trong cấu hình mã nguồn (tại `DatabaseConnection.java` hoặc file properties tương ứng).

### Bước 2: Build dự án bằng Maven

Mở Terminal tại thư mục gốc của dự án và chạy lệnh:

```bash
mvn clean package

```

### Bước 3: Khởi chạy Server (Bắt buộc chạy trước)

Mở một cửa sổ Terminal mới và gõ lệnh:

```bash
java -jar target/server.jar

```

*(Server sẽ bắt đầu lắng nghe tại cổng `8080` (hoặc cổng cấu hình). Hãy giữ nguyên cửa sổ terminal này).*

### Bước 4: Khởi chạy Client (Có thể mở nhiều cửa sổ)

Mở một hoặc nhiều cửa sổ Terminal mới (mỗi cửa sổ đóng vai trò là một người dùng) và gõ lệnh:

```bash
java -jar target/client.jar

```

Giao diện phần mềm JavaFX sẽ hiện lên và tự động kết nối Socket tới Server.

## 6. Danh sách chức năng đã hoàn thành

Theo barem đồ án, hệ thống đã xử lý và đạt được các tính năng cốt lõi sau:

* [x] **Áp dụng OOP & Design Patterns:** Sử dụng triệt để *Singleton* (Network), *Factory Method* (Tạo Item: Electronics, Art, Vehicle) và *Observer* (Realtime Update qua Socket).
* [x] **Kiến trúc mã sạch:** Injection thông qua Constructor (Dependency Injection cho ClientHandler), chia tầng DAO và Service.
* [x] **Đăng ký / Đăng nhập:** Phân quyền theo Bidder và Seller.
* [x] **Quản lý Sản phẩm & Phiên đấu giá:** Đăng bán, mở/đóng phiên đấu giá.
* [x] **Đấu giá thời gian thực (Real-time update):** Mọi lượt bid đều được đẩy ngay lập tức (Broadcast) đến tất cả các Client đang bật ứng dụng thông qua Socket mà không cần F5/Reload trang.
* [x] **Trực quan hóa lịch sử giá (Visualization):** Sử dụng JavaFX `LineChart` để vẽ biểu đồ sự biến thiên của giá theo thời gian thực.
* [x] **Chống bắn tỉa (Anti-sniping):** Cập nhật quy tắc cộng thêm thời gian nếu có lượt Bid ở những giây cuối cùng.
* [x] **Bảo vệ luồng (Thread-safe):** Chặn đứng nguy cơ Race-condition và Lost-update khi nhiều luồng cùng ghi giá.
* [x] **Tích hợp CI/CD & Testing:** Có workflow GitHub Actions để chạy bộ bài kiểm thử JUnit 5 + Mockito 100% không dính líu DB thật.
* *Chú ý:* Nhóm đã quyết định loại bỏ tính năng *Auto-Bidding* theo thiết kế mới nhất để tập trung đảm bảo tính ổn định và an toàn tài nguyên mạng (Không bị dính lỗi NullPointerException hoặc ngập lụt Server).

## 7. Tài liệu và Liên kết

* **Báo cáo tổng kết (PDF):** [Xem tại Google Drive](https://drive.google.com/file/d/1myCkzWTOL_62xOK1ZDLG8Mh1yIa2vdyB/view?usp=sharing)
* **Video Demo Hệ thống:** [Xem tại Youtube](https://www.youtube.com/watch?v=j7fX-XHUH9o)

---

**Nhóm 14 Lập trình Nâng cao**
