package com.auction.service;

import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;
import com.auction.dto.RegisterRequest;
import com.auction.dto.RegisterResponse;

/**
 * Service interface for user authentication and registration operations.
 * Provides contracts for login and registration functionality.
 */
public interface UserService {

    /**
     * Authenticates a user based on provided login credentials.
     *
     * @param request the {@link LoginRequest} containing username and password
     * @return {@link LoginResponse} with authentication result and user details if successful
     * @throws IllegalArgumentException if request is null
     */
    LoginResponse login(LoginRequest request);

    /**
     * Registers a new user account with the provided credentials.
     *
     * @param request the {@link RegisterRequest} containing username, password, confirmation password, and role
     * @return {@link RegisterResponse} with registration result and message
     * @throws IllegalArgumentException if request is null
     */
    RegisterResponse register(RegisterRequest request);
}

