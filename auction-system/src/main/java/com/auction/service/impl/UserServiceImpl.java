package com.auction.service.impl;

import com.auction.dao.UserDAO;
import com.auction.dto.*;
import com.auction.model.user.User;
import com.auction.service.UserService;

public class UserServiceImpl implements UserService {

    private static final String ERROR_MISSING_CREDENTIALS = "Vui lòng nhập đủ thông tin.";
    private static final String ERROR_INVALID_USERNAME = "Sai tên đăng nhập.";
    private static final String ERROR_INVALID_PASSWORD = "Sai mật khẩu.";
    private static final String ERROR_USERNAME_EXISTS = "Tên đăng nhập đã tồn tại.";
    private static final String ERROR_PASSWORD_MISMATCH = "Mật khẩu xác nhận không khớp.";
    private static final String ERROR_SAVE_FAILED = "Lỗi hệ thống, không thể lưu tài khoản.";

    private static final String SUCCESS_LOGIN = "Đăng nhập thành công.";
    private static final String SUCCESS_REGISTER = "Đăng ký thành công.";

    private final UserDAO userDAO;

    // Không cần truyền UserRepository nếu không dùng, khởi tạo trực tiếp DAO
    public UserServiceImpl(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    @Override
    public LoginResponse login(LoginRequest req) {
        // BỔ SUNG: Kiểm tra dữ liệu đầu vào
        if (req == null || isCredentialsInvalid(req.getUsername(), req.getPassword())) {
            return new LoginResponse(false, ERROR_MISSING_CREDENTIALS, null, null);
        }

        try {
            User user = userDAO.authenticate(req.getUsername(), req.getPassword());
            // SỬA: Trả về Role (tên class) thay vì trả về mật khẩu
            String role = user.getClass().getSimpleName().toUpperCase();
            return new LoginResponse(true, SUCCESS_LOGIN, user.getUsername(), role);

        } catch (Exception e) {
            String msg = e.getMessage();
            // SỬA: Dùng .equals() thay vì ==
            if ("INVALID_USERNAME".equals(msg)) {
                return new LoginResponse(false, ERROR_INVALID_USERNAME, null, null);
            } else if ("INVALID_PASSWORD".equals(msg)) {
                return new LoginResponse(false, ERROR_INVALID_PASSWORD, null, null);
            }
            return new LoginResponse(false, "ngu", null, null);
        }
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        if (request == null || isCredentialsInvalid(request.getUsername(), request.getPassword())) {
            return new RegisterResponse(false, ERROR_MISSING_CREDENTIALS);
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return new RegisterResponse(false, ERROR_PASSWORD_MISMATCH);
        }

        try {
            // Biến registering không dùng có thể bỏ qua
            userDAO.registerUser(request.getUsername(), request.getPassword(), request.getRole());
            return new RegisterResponse(true, SUCCESS_REGISTER);
        } catch (Exception e) {
            String msg = e.getMessage();
            // SỬA: Dùng .equals() và khớp chính xác chữ USERNAME_EXIST từ DAO
            if ("USERNAME_EXIST".equals(msg)) {
                return new RegisterResponse(false, ERROR_USERNAME_EXISTS);
            } else if ("SAVE_FAILED".equals(msg)) {
                return new RegisterResponse(false, ERROR_SAVE_FAILED);
            } else {
                return new RegisterResponse(false, msg);
            }
        }
    }

    private boolean isCredentialsInvalid(String username, String password) {
        return username == null || username.isBlank() ||
                password == null || password.isBlank();
    }
}