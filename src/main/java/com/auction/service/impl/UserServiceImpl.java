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

	public UserServiceImpl(UserDAO userDAO) {
		this.userDAO = userDAO;
	}

	@Override
	public LoginResponse login(LoginRequest req) {
		if (req == null || isCredentialsInvalid(req.getUsername(), req.getPassword())) {
			return new LoginResponse(false, ERROR_MISSING_CREDENTIALS, null, null, null);
		}

		try {
			User user = userDAO.authenticate(req.getUsername(), req.getPassword());
			String role = user.getClass().getSimpleName().toUpperCase();

			// SỬA: Đã thêm user.getId() vào Constructor
			return new LoginResponse(true, SUCCESS_LOGIN, user.getId(), user.getUsername(), role);

		} catch (Exception e) {
			String msg = e.getMessage();
			if ("INVALID_USERNAME".equals(msg)) {
				return new LoginResponse(false, ERROR_INVALID_USERNAME, null, null, null);
			} else if ("INVALID_PASSWORD".equals(msg)) {
				return new LoginResponse(false, ERROR_INVALID_PASSWORD, null, null, null);
			}
			return new LoginResponse(false, "Lỗi hệ thống: " + msg, null, null, null);
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
			userDAO.registerUser(request.getUsername(), request.getPassword(), request.getRole());
			return new RegisterResponse(true, SUCCESS_REGISTER);
		} catch (Exception e) {
			String msg = e.getMessage();
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
		return username == null || username.isBlank() || password == null || password.isBlank();
	}
}