package com.auction.client;

/**
 * Singleton lưu thông tin user đang đăng nhập trong phiên client.
 * Các controller có thể đọc Session.get() để biết userId, role, username.
 */
public class Session {

    private static Session instance;

    private Long userId;
    private String username;
    private String role; // BIDDER | SELLER | ADMIN

    // Trạng thái phụ trợ cho điều hướng (ví dụ: xem chi tiết phiên đấu giá nào)
    private Long selectedAuctionId;
    private Long selectedItemId;

    private Session() {}

    public static Session get() {
        if (instance == null) instance = new Session();
        return instance;
    }

    public void loginAs(Long userId, String username, String role) {
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public void logout() {
        this.userId = null;
        this.username = null;
        this.role = null;
        this.selectedAuctionId = null;
        this.selectedItemId = null;
    }

    public boolean isLoggedIn() { return userId != null; }
    public boolean isBidder()   { return "BIDDER".equalsIgnoreCase(role); }
    public boolean isSeller()   { return "SELLER".equalsIgnoreCase(role); }
    public boolean isAdmin()    { return "ADMIN".equalsIgnoreCase(role); }

    public Long getUserId()       { return userId; }
    public String getUsername()   { return username; }
    public String getRole()       { return role; }

    public Long getSelectedAuctionId() { return selectedAuctionId; }
    public void setSelectedAuctionId(Long id) { this.selectedAuctionId = id; }

    public Long getSelectedItemId() { return selectedItemId; }
    public void setSelectedItemId(Long id) { this.selectedItemId = id; }
}
