package com.auction.service.impl;

import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;
import com.auction.dto.RegisterRequest;
import com.auction.dto.RegisterResponse;
import com.auction.model.user.User;
import com.auction.repository.UserRepository;
import com.auction.service.UserService;

/**
 * Implementation of UserService for user authentication and registration.
 * Handles login and registration operations with validation.
 */
public class UserServiceImpl implements UserService {
    
    // Error messages
    private static final String ERROR_MISSING_CREDENTIALS = "Vui lòng nhập đủ thông tin.";
    private static final String ERROR_INVALID_USERNAME = "Sai tên đăng nhập.";
    private static final String ERROR_INVALID_PASSWORD = "Sai mật khẩu.";
    private static final String ERROR_USERNAME_EXISTS = "Tên đăng nhập đã tồn tại.";
    private static final String ERROR_PASSWORD_MISMATCH = "Mật khẩu xác nhận không khớp.";
    private static final String ERROR_SAVE_FAILED = "Lỗi hệ thống, không thể lưu tài khoản.";
    
    // Success messages
    private static final String SUCCESS_LOGIN = "Đăng nhập thành công.";
    private static final String SUCCESS_REGISTER = "Đăng ký thành công.";
    
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Authenticates a user based on username and password.
     *
     * @param request the login request containing username and password
     * @return LoginResponse with success status and user details if successful
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        // Validate input
        if (request == null || isCredentialsInvalid(request.getUsername(), request.getPassword())) {
            return new LoginResponse(false, ERROR_MISSING_CREDENTIALS, null, null);
        }

        // Find user by username
        User user = userRepository.findByUsername(request.getUsername());
        if (user == null) {
            return new LoginResponse(false, ERROR_INVALID_USERNAME, null, null);
        }

        // Verify password
        if (!user.getPassword().equals(request.getPassword())) {
            return new LoginResponse(false, ERROR_INVALID_PASSWORD, null, null);
        }

        // Successful login
        return new LoginResponse(true, SUCCESS_LOGIN, user.getUsername(), user.getRole());
    }

    /**
     * Registers a new user with validation.
     *
     * @param request the registration request containing username, password, and role
     * @return RegisterResponse with success status and message
     */
    @Override
    public RegisterResponse register(RegisterRequest request) {
        // Validate input
        if (request == null || isCredentialsInvalid(request.getUsername(), request.getPassword())) {
            return new RegisterResponse(false, ERROR_MISSING_CREDENTIALS);
        }

        // Verify password confirmation
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return new RegisterResponse(false, ERROR_PASSWORD_MISMATCH);
        }

        // Check if username already exists
        User existingUser = userRepository.findByUsername(request.getUsername());
        if (existingUser != null) {
            return new RegisterResponse(false, ERROR_USERNAME_EXISTS);
        }

        // Create and save new user
        User newUser = new User(0, request.getUsername(), request.getPassword(), request.getRole());
        boolean isSaved = userRepository.saveUser(newUser);

        if (isSaved) {
            return new RegisterResponse(true, SUCCESS_REGISTER);
        } else {
            return new RegisterResponse(false, ERROR_SAVE_FAILED);
        }
    }

    /**
     * Validates that credentials are not null, empty, or blank.
     *
     * @param username the username to validate
     * @param password the password to validate
     * @return true if credentials are invalid, false otherwise
     */
    private boolean isCredentialsInvalid(String username, String password) {
        return username == null || username.isBlank() || 
               password == null || password.isBlank();
    }
}
