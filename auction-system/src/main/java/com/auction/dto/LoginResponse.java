package com.auction.dto;

public class LoginResponse {
    private boolean success;
    private String message;
    private String username;
    private String role;// Tên user trả về để giao diện hiển thị "Xin chào, ..."

    // Constructors
    public LoginResponse(boolean success, String message, String username,String role) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.role = role;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
}