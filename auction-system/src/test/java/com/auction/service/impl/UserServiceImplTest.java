package com.auction.service.impl;

import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;
import com.auction.dto.RegisterRequest;
import com.auction.dto.RegisterResponse;
import com.auction.model.user.User;
import com.auction.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserServiceImpl.
 * Tests login and registration functionality with various scenarios.
 */
@DisplayName("UserServiceImpl Tests")
class UserServiceImplTest {

    // Constants for test data
    private static final String VALID_USERNAME = "testUser";
    private static final String VALID_PASSWORD = "correctPassword";
    private static final String INVALID_PASSWORD = "wrongPassword";
    private static final String VALID_ROLE = "BIDDER";
    private static final Long VALID_USER_ID = 0L;

    // Constants for error messages
    private static final String MISSING_CREDENTIALS_MSG = "Please provide complete credentials.";
    private static final String INVALID_USERNAME_MSG = "Invalid username.";
    private static final String INVALID_PASSWORD_MSG = "Invalid password.";
    private static final String USERNAME_EXISTS_MSG = "Username already exists.";
    private static final String PASSWORD_MISMATCH_MSG = "Passwords do not match.";
    private static final String SAVE_FAILED_MSG = "System error, unable to save account.";
    private static final String REGISTRATION_SUCCESS_MSG = "Registration successful.";
    private static final String LOGIN_SUCCESS_MSG = "Login successful.";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new User(VALID_USER_ID, VALID_USERNAME, VALID_PASSWORD, VALID_ROLE);
    }

    // ============ LOGIN TESTS ============

    @Test
    @DisplayName("Login with empty username should return error")
    void testLoginEmptyUsername() {
        // Arrange
        LoginRequest request = new LoginRequest("", "password");

        // Act
        LoginResponse response = userService.login(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(MISSING_CREDENTIALS_MSG, response.getMessage());
    }

    @Test
    @DisplayName("Login with empty password should return error")
    void testLoginEmptyPassword() {
        // Arrange
        LoginRequest request = new LoginRequest("testuser", "");

        // Act
        LoginResponse response = userService.login(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(MISSING_CREDENTIALS_MSG, response.getMessage());
    }

    @Test
    @DisplayName("Login with empty credentials should return error")
    void testLoginEmptyCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest("", "");

        // Act
        LoginResponse response = userService.login(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(MISSING_CREDENTIALS_MSG, response.getMessage());
    }

    @Test
    @DisplayName("Login with non-existent user should return error")
    void testLoginNonExistentUser() {
        // Arrange
        LoginRequest request = new LoginRequest("wrongUser", VALID_PASSWORD);
        when(userRepository.findByUsername("wrongUser")).thenReturn(null);

        // Act
        LoginResponse response = userService.login(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(INVALID_USERNAME_MSG, response.getMessage());
        verify(userRepository, times(1)).findByUsername("wrongUser");
    }

    @Test
    @DisplayName("Login with incorrect password should return error")
    void testLoginIncorrectPassword() {
        // Arrange
        LoginRequest request = new LoginRequest(VALID_USERNAME, INVALID_PASSWORD);
        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(testUser);

        // Act
        LoginResponse response = userService.login(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(INVALID_PASSWORD_MSG, response.getMessage());
    }

    @Test
    @DisplayName("Login with valid credentials should return success")
    void testLoginWithValidCredentials() {
        // Arrange
        LoginRequest request = new LoginRequest(VALID_USERNAME, VALID_PASSWORD);
        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(testUser);

        // Act
        LoginResponse response = userService.login(request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(LOGIN_SUCCESS_MSG, response.getMessage());
        assertEquals(VALID_USERNAME, response.getUsername());
        assertEquals(VALID_ROLE, response.getRole());
    }

    @Test
    @DisplayName("Login should retrieve user from repository")
    void testLoginRepositoryInteraction() {
        // Arrange
        LoginRequest request = new LoginRequest(VALID_USERNAME, VALID_PASSWORD);
        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(testUser);

        // Act
        userService.login(request);

        // Assert
        verify(userRepository, times(1)).findByUsername(VALID_USERNAME);
    }

    // ============ REGISTRATION TESTS ============

    @Test
    @DisplayName("Register with empty username should return error")
    void testRegisterEmptyUsername() {
        // Arrange
        RegisterRequest request = new RegisterRequest("", "password", "password", VALID_ROLE);

        // Act
        RegisterResponse response = userService.register(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(MISSING_CREDENTIALS_MSG, response.getMessage());
    }

    @Test
    @DisplayName("Register with empty password should return error")
    void testRegisterEmptyPassword() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "", "", VALID_ROLE);

        // Act
        RegisterResponse response = userService.register(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(MISSING_CREDENTIALS_MSG, response.getMessage());
    }

    @Test
    @DisplayName("Register with empty credentials should return error")
    void testRegisterEmptyCredentials() {
        // Arrange
        RegisterRequest request = new RegisterRequest("", "", "", VALID_ROLE);

        // Act
        RegisterResponse response = userService.register(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(MISSING_CREDENTIALS_MSG, response.getMessage());
    }

    @Test
    @DisplayName("Register with mismatched passwords should return error")
    void testRegisterMismatchedPasswords() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "password123", "password456", VALID_ROLE);

        // Act
        RegisterResponse response = userService.register(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(PASSWORD_MISMATCH_MSG, response.getMessage());
    }

    @Test
    @DisplayName("Register with existing username should return error")
    void testRegisterExistingUsername() {
        // Arrange
        RegisterRequest request = new RegisterRequest(VALID_USERNAME, "password", "password", VALID_ROLE);
        when(userRepository.findByUsername(VALID_USERNAME)).thenReturn(testUser);

        // Act
        RegisterResponse response = userService.register(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(USERNAME_EXISTS_MSG, response.getMessage());
        verify(userRepository, times(1)).findByUsername(VALID_USERNAME);
    }

    @Test
    @DisplayName("Register when database save fails should return error")
    void testRegisterDatabaseSaveFails() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "password123", "password123", VALID_ROLE);
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(userRepository.saveUser(any(User.class))).thenReturn(false);

        // Act
        RegisterResponse response = userService.register(request);

        // Assert
        assertFalse(response.isSuccess());
        assertEquals(SAVE_FAILED_MSG, response.getMessage());
        verify(userRepository, times(1)).saveUser(any(User.class));
    }

    @Test
    @DisplayName("Register with valid data should return success")
    void testRegisterWithValidData() {
        // Arrange
        String newUsername = "newuser";
        String newPassword = "password123";
        String newRole = "SELLER";
        RegisterRequest request = new RegisterRequest(newUsername, newPassword, newPassword, newRole);
        when(userRepository.findByUsername(newUsername)).thenReturn(null);
        when(userRepository.saveUser(any(User.class))).thenReturn(true);

        // Act
        RegisterResponse response = userService.register(request);

        // Assert
        assertTrue(response.isSuccess());
        assertEquals(REGISTRATION_SUCCESS_MSG, response.getMessage());
        
        verify(userRepository, times(1)).findByUsername(newUsername);
        verify(userRepository, times(1)).saveUser(argThat(user ->
            user.getUsername().equals(newUsername) &&
            user.getPassword().equals(newPassword) &&
            user.getRole().equals(newRole)
        ));
    }

    @Test
    @DisplayName("Register should check for existing username first")
    void testRegisterRepositoryUsernameCheck() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "password", "password", VALID_ROLE);
        when(userRepository.findByUsername("newuser")).thenReturn(null);
        when(userRepository.saveUser(any(User.class))).thenReturn(true);

        // Act
        userService.register(request);

        // Assert
        verify(userRepository, times(1)).findByUsername("newuser");
    }

    @Test
    @DisplayName("Register should not save if password confirmation fails")
    void testRegisterNoSaveOnPasswordMismatch() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "password1", "password2", VALID_ROLE);

        // Act
        userService.register(request);

        // Assert
        verify(userRepository, never()).saveUser(any(User.class));
    }
}

