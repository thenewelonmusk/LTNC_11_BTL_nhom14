# Fix: Too Many Connections - Connection Pooling Implementation

## Vấn Đề Ban Đầu

```
java.sql.SQLNonTransientConnectionException: Data source rejected establishment of connection,
message from server: "Too many connections"
```

**Nguyên Nhân:** Mỗi lần gọi `getConnection()` đều tạo một **kết nối mới** với database mà không có reuse. Khi có nhiều requests liên tiếp, MySQL bị quá tải vì vượt quá max connections (MySQL default: 151).

---

## Giải Pháp: HikariCP Connection Pooling

### 1. Nguyên Lý Hoạt Động

**Trước (Cơ chế cũ):**
```
Request 1 → New Connection → Query → Close
Request 2 → New Connection → Query → Close
Request 3 → New Connection → Query → Close
...
(Mỗi request tạo kết nối mới = chậm + tốn resources)
```

**Sau (Connection Pooling):**
```
Pool: [Conn1] [Conn2] [Conn3] [Conn4] [Conn5] (5-10 connections được tái sử dụng)

Request 1 → Borrow Conn1 → Query → Return Conn1
Request 2 → Borrow Conn2 → Query → Return Conn2
Request 3 → Borrow Conn1 (available) → Query → Return Conn1
Request 4 → Borrow Conn3 → Query → Return Conn3
...
(Tái sử dụng kết nối = nhanh + tiết kiệm resources)
```

### 2. Các Thay Đổi

#### 2.1 pom.xml - Thêm HikariCP Dependency

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

#### 2.2 DatabaseConnection.java - Rewrite với HikariCP

**Khởi tạo Pool (Static Initializer):**
```java
static {
    initializePool();
}

private static void initializePool() {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:mysql://localhost:3306/auction_db?...");
    config.setUsername("root");
    config.setPassword("password");
    
    // Cấu hình quan trọng:
    config.setMaximumPoolSize(10);        // Tối đa 10 connections
    config.setMinimumIdle(5);             // Tối thiểu 5 connections rảnh
    config.setConnectionTimeout(30000);   // Timeout 30 giây
    config.setIdleTimeout(600000);        // Đóng nếu idle 10 phút
    config.setMaxLifetime(1800000);       // Tuổi tối đa: 30 phút
    
    hikariDataSource = new HikariDataSource(config);
}
```

**Lấy Connection (Reuse từ Pool):**
```java
public static Connection getConnection() {
    // Từ transaction (nếu có)
    Connection conn = transactionConnection.get();
    if (conn != null && !conn.isClosed()) {
        return conn;
    }
    
    // Từ pool (reuse hoặc tạo mới nếu cần)
    return hikariDataSource.getConnection();
}
```

**Transaction Support:**
```java
public static Connection beginTransaction() throws SQLException {
    Connection conn = hikariDataSource.getConnection();
    conn.setAutoCommit(false);
    transactionConnection.set(conn);  // Lưu vào ThreadLocal
    return conn;
}

public static void commit() throws SQLException {
    Connection conn = transactionConnection.get();
    if (conn != null) {
        conn.commit();
        conn.setAutoCommit(true);
        conn.close();  // Return về pool (quan trọng!)
        transactionConnection.remove();
    }
}
```

#### 2.3 AuctionServer.java - Shutdown Hook

```java
// Khi server tắt, đóng pool gracefully
Runtime.getRuntime().addShutdownHook(new Thread(() -> {
    System.out.println("[*] Server đang tắt...");
    pool.shutdown();
    scheduler.shutdown();
    DatabaseConnection.closePool();  // ← Close pool
    System.out.println("[✓] Cleanup hoàn tất");
}));
```

---

## 3. Cấu Hình Chi Tiết

### HikariCP Config Explanation

| Tham số | Giá Trị | Ý Nghĩa |
|---------|--------|---------|
| `maximumPoolSize` | 10 | Tối đa 10 connections có thể mở cùng lúc |
| `minimumIdle` | 5 | Giữ ít nhất 5 connections rảnh sẵn sàng |
| `connectionTimeout` | 30000ms | Chờ tối đa 30 giây để lấy connection |
| `idleTimeout` | 600000ms | Đóng connection sau 10 phút không sử dụng |
| `maxLifetime` | 1800000ms | Connection tối đa 30 phút rồi phải tạo lại |
| `leakDetectionThreshold` | 60000ms | Cảnh báo nếu connection bị leak > 60 giây |

### MySQL max_connections

Kiểm tra cấu hình MySQL:
```sql
-- Xem max connections hiện tại
SHOW VARIABLES LIKE 'max_connections';

-- Tăng max connections (nếu cần)
SET GLOBAL max_connections = 1000;
```

---

## 4. Flow Diagram

```
┌─────────────────────────────────────────────────────────┐
│ Client Request (Login / Bid / etc)                      │
└──────────────────┬──────────────────────────────────────┘
                   │
                   ↓
         ┌─────────────────────┐
         │ DAO.getUserById()   │
         │ (hoặc operation khác)│
         └──────────┬──────────┘
                    │
                    ↓
         ┌─────────────────────────────────────┐
         │ Connection conn =                    │
         │ DatabaseConnection.getConnection()   │
         └──────────────┬──────────────────────┘
                        │
              ┌─────────┴─────────┐
              ↓                   ↓
      ┌──────────────┐   ┌─────────────────┐
      │ In Transaction?
      │ (ThreadLocal)
      │               │   │ No → Use Pool   │
      │ Yes → Use TL  │   └────────┬────────┘
      └──────┬────────┘            │
             │                     ↓
             │            ┌──────────────────┐
             │            │ HikariCP Pool    │
             │            │ [C1][C2]...[C10] │
             │            │ ↓ borrow ↑ return│
             │            └────┬─────────────┘
             │                 │
             └─────────┬───────┘
                       ↓
         ┌──────────────────────────┐
         │ Execute Query            │
         │ (PreparedStatement, etc)  │
         └──────────────┬───────────┘
                        │
              ┌─────────┴──────────┐
              ↓                    ↓
      ┌──────────────┐   ┌──────────────────┐
      │ In Transaction?│   │ No Transaction   │
      │ conn.close() ← │   │ conn.close()  ←  │
      │ (keeps TL)   │   │ (return to pool) │
      └──────────────┘   └──────────────────┘
```

---

## 5. Performance Improvement

### Trước (Direct DriverManager)
```
1 Request = 1 connection creation (~100ms overhead)
100 concurrent = 100 new connections (fail if > MySQL limit)
```

### Sau (HikariCP Pool)
```
1 Request = borrow from pool (~1ms overhead)
100 concurrent = reuse 5-10 connections (optimal resource usage)
No "Too many connections" errors
```

---

## 6. Troubleshooting

### Q: Connection bị leak (warning logs)?
**A:** Đảm bảo mọi `try-with-resources` hoặc `.close()` được gọi:
```java
// ✅ Đúng
try (Connection conn = DatabaseConnection.getConnection()) {
    // Use conn
}

// ✅ Đúng
Connection conn = DatabaseConnection.getConnection();
try {
    // Use conn
} finally {
    conn.close();  // Important!
}

// ❌ Sai (leak)
Connection conn = DatabaseConnection.getConnection();
// Use conn but forgot to close
```

### Q: "Too many connections" vẫn xảy ra?
**A:** 
1. Kiểm tra debug logs: `DatabaseConnection.printPoolStats()`
2. Tăng `maxPoolSize` từ 10 lên 20
3. Kiểm tra có query nào chạy lâu không (locking connections)

### Q: Pool bị "Evicted from pool"?
**A:** Normal - HikariCP tự động tái tạo connections nếu bị evicted

---

## 7. Testing

### Test Connection Pool

```java
// Server startup sẽ in:
// [✓] HikariCP Connection Pool khởi tạo thành công!
//     - Max connections: 10
//     - Min idle: 5

// Để debug pool stats:
DatabaseConnection.printPoolStats();
// Output:
// === HikariCP Pool Stats ===
// Total connections: 5
// Active connections: 2
// Idle connections: 3
// Pending threads: 0
```

### Test Login Flow
1. Server khởi động → Pool initialized (5 resting connections)
2. Client 1 login → Borrow Conn1 → Authenticate → Return Conn1
3. Client 2 login → Borrow Conn2 → Authenticate → Return Conn2
4. Multiple logins → Reuse Conn1, Conn2, etc. → No "Too many connections"!

---

## 8. Best Practices

✅ **Luôn Close Connections**
```java
Connection conn = DatabaseConnection.getConnection();
try {
    // Use conn
} finally {
    conn.close();  // Returns to pool
}
```

✅ **Use Try-with-Resources**
```java
try (Connection conn = DatabaseConnection.getConnection()) {
    // Auto-close
}
```

✅ **Transaction Management**
```java
try {
    DatabaseConnection.beginTransaction();
    // Multiple queries
    DatabaseConnection.commit();
} catch (Exception e) {
    DatabaseConnection.rollback();
}
```

❌ **Tránh Nested Transactions**
```java
// ❌ Không làm cái này
DatabaseConnection.beginTransaction();
DatabaseConnection.beginTransaction();  // Bug!
```

---

## 9. Summary

| Aspect | Trước | Sau |
|--------|-------|-----|
| **Connection Creation** | Mỗi request | Pool reuse |
| **Max Connections** | Unlimited (fail) | Controlled (10) |
| **Response Time** | 100ms+ overhead | <1ms overhead |
| **Resource Usage** | High (new conn/req) | Low (reuse) |
| **Error "Too many conn"** | Thường xuyên ❌ | Không xảy ra ✅ |

Giải pháp này đảm bảo ứng dụng **ổn định, nhanh chóng, và không bị vượt quá giới hạn database connections**.
