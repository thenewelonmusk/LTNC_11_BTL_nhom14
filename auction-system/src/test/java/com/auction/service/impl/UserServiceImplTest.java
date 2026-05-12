package com.auction.service.impl;

import com.auction.dao.UserDAO;
import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;
import com.auction.dto.RegisterRequest;
import com.auction.dto.RegisterResponse;
import com.auction.model.user.Bidder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class UserServiceImplTest {

    private UserDAO dao;
    private UserServiceImpl svc;

    @BeforeEach
    public void setup() {
        dao = mock(UserDAO.class);
        svc = new UserServiceImpl(dao);
    }

    @Test
    public void testLoginNullReq() {
        LoginResponse res = svc.login(null);
        assertFalse(res.isSuccess());
        assertEquals("Vui lòng nhập đủ thông tin.", res.getMessage());
    }

    @Test
    public void testLoginEmpty() {
        LoginRequest req = new LoginRequest("", "   ");
        LoginResponse res = svc.login(req);
        assertFalse(res.isSuccess());
    }

    @Test
    public void testLoginWrongUser() throws Exception {
        LoginRequest req = new LoginRequest("test", "123");
        when(dao.authenticate("test", "123")).thenThrow(new Exception("INVALID_USERNAME"));

        LoginResponse res = svc.login(req);
        assertFalse(res.isSuccess());
        assertEquals("Sai tên đăng nhập.", res.getMessage());
    }

    @Test
    public void testLoginWrongPass() throws Exception {
        LoginRequest req = new LoginRequest("test", "123");
        when(dao.authenticate("test", "123")).thenThrow(new Exception("INVALID_PASSWORD"));

        LoginResponse res = svc.login(req);
        assertFalse(res.isSuccess());
        assertEquals("Sai mật khẩu.", res.getMessage());
    }

    @Test
    public void testLoginOk() throws Exception {
        LoginRequest req = new LoginRequest("test", "123");
        Bidder b = new Bidder(1L, "test", "123");
        when(dao.authenticate("test", "123")).thenReturn(b);

        LoginResponse res = svc.login(req);
        assertTrue(res.isSuccess());
        assertEquals("BIDDER", res.getRole());
        assertEquals("test", res.getUsername());
    }

    @Test
    public void testRegNullReq() {
        RegisterResponse res = svc.register(null);
        assertFalse(res.isSuccess());
    }

    @Test
    public void testRegMismatch() {
        RegisterRequest req = new RegisterRequest("test", "123", "456", "BIDDER");
        RegisterResponse res = svc.register(req);
        assertFalse(res.isSuccess());
        assertEquals("Mật khẩu xác nhận không khớp.", res.getMessage());
    }

    @Test
    public void testRegExist() throws Exception {
        RegisterRequest req = new RegisterRequest("test", "123", "123", "BIDDER");
        when(dao.registerUser("test", "123", "BIDDER")).thenThrow(new Exception("USERNAME_EXIST"));

        RegisterResponse res = svc.register(req);
        assertFalse(res.isSuccess());
        assertEquals("Tên đăng nhập đã tồn tại.", res.getMessage());
    }

    @Test
    public void testRegFail() throws Exception {
        RegisterRequest req = new RegisterRequest("test", "123", "123", "BIDDER");
        when(dao.registerUser("test", "123", "BIDDER")).thenThrow(new Exception("SAVE_FAILED"));

        RegisterResponse res = svc.register(req);
        assertFalse(res.isSuccess());
        assertEquals("Lỗi hệ thống, không thể lưu tài khoản.", res.getMessage());
    }

    @Test
    public void testRegOk() throws Exception {
        RegisterRequest req = new RegisterRequest("test", "123", "123", "BIDDER");
        when(dao.registerUser("test", "123", "BIDDER")).thenReturn(true);

        RegisterResponse res = svc.register(req);
        assertTrue(res.isSuccess());
        assertEquals("Đăng ký thành công.", res.getMessage());
    }
}