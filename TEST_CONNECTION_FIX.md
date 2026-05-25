# Connection Leak Fix - Testing Guide

## Problem Fixed
- **File**: `BidDAO.java` - `findByAuctionId()` method
- **Issue**: Connection was NOT closed after query execution
- **Impact**: HikariCP pool exhaustion after several queries
- **Error**: `HikariPool-1 - Connection is not available, request timed out after 5001ms`

## Root Cause
```java
// ❌ BEFORE (Leak)
try {
    Connection conn = DatabaseConnection.getConnection();
    PreparedStatement stmt = conn.prepareStatement(sql);
    stmt.setLong(1, auctionId);
    ResultSet rs = stmt.executeQuery();
    while (rs.next()) {
        list.add(mapRow(rs));
    }
    rs.close();
    stmt.close();
    // ❌ Connection NEVER returned to pool!
} catch (Exception e) {
    e.printStackTrace();
}
```

## Solution Applied
```java
// ✅ AFTER (Fixed)
try (Connection conn = DatabaseConnection.getConnection();
     PreparedStatement stmt = conn.prepareStatement(sql)) {
    stmt.setLong(1, auctionId);
    try (ResultSet rs = stmt.executeQuery()) {
        while (rs.next()) {
            list.add(mapRow(rs));
        }
    }
    // ✅ Auto-closes both stmt and conn via try-with-resources
} catch (Exception e) {
    e.printStackTrace();
}
```

## Verification Steps

### Step 1: Start Server
```bash
cd c:\Users\ADMIN\Downloads\LTNC_11_BTL_nhom141
java -cp target/classes:target/dependency/* com.auction.server.AuctionServer
```

### Step 2: Monitor Connection Pool
Watch for log messages like:
```
[DEBUG] HikariPool-1: Connection created
[DEBUG] HikariPool-1: Active connections: 1, Idle connections: 9
```

### Step 3: Run Client
- Login to system
- Browse auctions
- Place multiple bids
- Check if status updates continuously in table

### Step 4: Expected Behavior
✅ **Status monitor continues running** (prints every second)
✅ **No "Connection timeout" errors** in console
✅ **Active connections return to idle** after each query
✅ **Pool never exceeds 10 connections**

### Step 5: Success Criteria
- [ ] Server runs for 5+ minutes without timeout
- [ ] Multiple clients can login simultaneously
- [ ] Auction status updates appear in real-time
- [ ] No connection leak warnings in logs

## Connection Pool Configuration
```
maxPoolSize: 10
minIdle: 5
connectionTimeout: 5000ms
idleTimeout: 600000ms (10 min)
leakDetectionThreshold: 60000ms
```

## Files Modified
1. `BidDAO.java` - `findByAuctionId()` - Fixed try-with-resources

## Files Already Fixed (Previous Session)
1. `AuctionDAO.java` - All methods use try-with-resources
2. `UserDAO.java` - All methods use try-with-resources
3. `ItemDAO.java` - All methods use try-with-resources
4. `DatabaseConnection.java` - HikariCP pooling implemented
5. `AuctionServer.java` - Graceful shutdown hook added

## Troubleshooting

### If still getting timeouts:
1. Check if other DAO methods close connections properly
2. Run `DatabaseConnection.printPoolStats()` to monitor pool status
3. Check logs for which method is causing the timeout

### Recommended Monitoring
Add this to AuctionServer to track pool health:
```java
ScheduledExecutorService monitoring = Executors.newScheduledThreadPool(1);
monitoring.scheduleAtFixedRate(() -> {
    try {
        DatabaseConnection.printPoolStats();
    } catch (Exception e) {
        e.printStackTrace();
    }
}, 0, 5, TimeUnit.SECONDS);
```

## Related Configuration Files
- `pom.xml` - HikariCP dependency
- `DatabaseConnection.java` - HikariCP initialization
- `REALTIME_STATUS_UPDATE_GUIDE.md` - Previous fix
- `CONNECTION_POOLING_HIKARICP.md` - Pool configuration details
