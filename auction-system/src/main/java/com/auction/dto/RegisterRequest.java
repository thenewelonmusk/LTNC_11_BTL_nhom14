package com.auction.dto;

public class RegisterRequest {
    private String username;
    private String password;
    private String confirmPassword;
    private String role;

    public String getUsername() {return username;}
    public String getPassword() {return password;}
    public String getConfirmPassword() {return confirmPassword;}
    public String getRole() {return role;}
}
