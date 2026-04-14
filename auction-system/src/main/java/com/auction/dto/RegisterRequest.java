package com.auction.dto;

public class RegisterRequest {
    private String username;
    private String password;
    private String confirmPassword;
    private String role;

    public RegisterRequest(String username, String password, String confirmPassword, String role) {
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.role = role;
    }

    public String getUsername() {return username;}
    public String getPassword() {return password;}
    public String getConfirmPassword() {return confirmPassword;}
    public String getRole() {return role;}
}
