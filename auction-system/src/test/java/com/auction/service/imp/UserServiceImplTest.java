package com.auction.service.imp;

// imports:

import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;
import com.auction.dto.RegisterRequest;
import com.auction.dto.RegisterResponse;
import com.auction.model.user.User;
import com.auction.repository.UserRepository;
import com.auction.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User(1,"testUser","correctPassword","BIDDER");
    }

    @Test
    void login_EmptyCredentials_ReturnsError() {
        LoginRequest request = new LoginRequest("","");
        LoginResponse response = userService.login(request);

        assertFalse(response.isSuccess());
        assertEquals("Vui lòng nhập đủ thông tin.",response.getMessage());
    }

    @Test
    void login_NonExistentUser_ReturnsError() {
        LoginRequest request = new LoginRequest("wrongUser","correctPassword");
        when(userRepository.findByUsername("wrongUser")).thenReturn(null);
        LoginResponse response = userService.login(request);

        assertFalse(response.isSuccess());
        assertEquals("Sai tên đăng nhập.",response.getMessage());
    }

    @Test
    void login_WrongPassword_ReturnsError() {
        LoginRequest request = new LoginRequest("testUser","wrongPassword");
        when(userRepository.findByUsername("testUser")).thenReturn(mockUser);
        LoginResponse response = userService.login(request);

        assertFalse(response.isSuccess());
        assertEquals("Sai mật khẩu.",response.getMessage());
    }

    @Test
    void login_ValidCredentials_ReturnsSuccess() {
        LoginRequest request = new LoginRequest("testUser","correctPassword");
        when(userRepository.findByUsername("testUser")).thenReturn(mockUser);
        LoginResponse response = userService.login(request);

        assertTrue(response.isSuccess());
        assertEquals("Đăng nhập thành công.",response.getMessage());
        assertEquals("testUser",response.getUsername());
        assertEquals("BIDDER",response.getRole());
    }

    @Test
    void register_EmptyCredentials_ReturnsError() {
        RegisterRequest request = new RegisterRequest("","","","BIDDER");
        RegisterResponse response = userService.register(request);

        assertFalse(response.isSuccess());
        assertEquals("Vui lòng nhập đủ thông tin.",response.getMessage());
    }

    @Test
    void register_MismatchedPassword_ReturnsError() {
        RegisterRequest request = new RegisterRequest("testUser","correctPassword","diffPassword","BIDDER");
        RegisterResponse response = userService.register(request);

        assertFalse(response.isSuccess());
        assertEquals("Mật khẩu xác nhận không khớp.",response.getMessage());
    }

    @Test
    void register_WithExistingUsername_ReturnsError() {
        RegisterRequest request = new RegisterRequest("testUser", "password", "password", "BIDDER");
        when(userRepository.findByUsername("testUser")).thenReturn(mockUser);
        RegisterResponse response = userService.register(request);

        assertFalse(response.isSuccess());
        assertEquals("Tên đăng nhập đã tồn tại.", response.getMessage());
    }

    @Test
    void register_WhenDatabaseSaveFails_ReturnsError() {
        RegisterRequest request = new RegisterRequest("newUser", "password", "password", "ROLE_USER");
        when(userRepository.findByUsername("newUser")).thenReturn(null);
        when(userRepository.saveUser(any(User.class))).thenReturn(false);
        RegisterResponse response = userService.register(request);

        assertFalse(response.isSuccess());
        assertEquals("Lỗi hệ thống, không thể lưu tài khoản.", response.getMessage());
    }

    @Test
    void register_WithValidData_ReturnsSuccess() {
        RegisterRequest request = new RegisterRequest("newUser", "password", "password", "ROLE_USER");
        when(userRepository.findByUsername("newUser")).thenReturn(null);
        when(userRepository.saveUser(any(User.class))).thenReturn(true);
        RegisterResponse response = userService.register(request);

        assertTrue(response.isSuccess());
        assertEquals("Đăng ký thành công.", response.getMessage());
        
        verify(userRepository, times(1)).saveUser(argThat(user ->
                user.getUsername().equals("newUser") &&
                        user.getPassword().equals("password") &&
                        user.getRole().equals("ROLE_USER")
        ));
    }
}
