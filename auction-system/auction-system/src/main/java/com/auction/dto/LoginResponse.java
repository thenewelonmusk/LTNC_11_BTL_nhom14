package com.auction.dto;

public class LoginResponse {
	private boolean success;
	private String message;
	private Long id; // Đã thêm id để Client biết ID của người dùng
	private String username;
	private String role;

	// Constructors
	public LoginResponse(boolean success, String message, Long id, String username, String role) {
		this.success = success;
		this.message = message;
		this.id = id;
		this.username = username;
		this.role = role;
	}

	// Getters
	public boolean isSuccess() {
		return success;
	}
	public String getMessage() {
		return message;
	}
	public Long getId() {
		return id;
	} // Getter cho id
	public String getUsername() {
		return username;
	}
	public String getRole() {
		return role;
	}
}