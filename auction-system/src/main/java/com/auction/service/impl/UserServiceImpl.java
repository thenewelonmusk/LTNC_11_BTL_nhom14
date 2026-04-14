package com.auction.service.impl;

import com.auction.dto.*;
import com.auction.model.user.User;
import com.auction.repository.UserRepository;
import com.auction.service.UserService;

public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public LoginResponse login (LoginRequest request){
        if (request.getUsername().isEmpty() || request.getPassword().isEmpty()) {
            return new LoginResponse(false, "Vui lòng nhập đủ thông tin.", null,null);
        }

        User user = userRepository.findByUsername(request.getUsername());

        if (user == null) {
            return new LoginResponse(false,"Sai tên đăng nhập.",null,null);
        }

        if (!user.getPassword().equals(request.getPassword())) {
            return new LoginResponse(false,"Sai mật khẩu.",null,null);
        }

        return new LoginResponse(true,"Đăng nhập thành công.",user.getUsername(),user.getRole());
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        if (request.getUsername().isEmpty() || request.getPassword().isEmpty()) {
            return new RegisterResponse(false,"Vui lòng nhập đủ thông tin.");
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            return new RegisterResponse(false,"Mật khẩu xác nhận không khớp.");
        }



        User existingUser = userRepository.findByUsername(request.getUsername());

        if (existingUser != null) {
            return new RegisterResponse(false,"Tên đăng nhập đã tồn tại.");
        }

        User newUser = new User(0,request.getUsername(),request.getPassword(),request.getRole());

        boolean isSaved = userRepository.saveUser(newUser);

        if (isSaved) {
            return new RegisterResponse(true,"Đăng ký thành công.");
        } else {
            return new RegisterResponse(false,"Lỗi hệ thống, không thể lưu tài khoản.");
        }
    }
}
