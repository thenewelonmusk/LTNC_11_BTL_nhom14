package com.auction.service;
import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;
import com.auction.dto.RegisterRequest;
import com.auction.dto.RegisterResponse;

public interface UserService {
    LoginResponse login(LoginRequest request);

    RegisterResponse register(RegisterRequest request);
}
