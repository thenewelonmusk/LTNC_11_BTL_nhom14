//package com.auction.server;
//
//import com.auction.dto.LoginRequest;
//import com.auction.dto.LoginResponse;
//import com.auction.dto.RegisterRequest;
//import com.auction.dto.RegisterResponse;
//import com.auction.service.UserService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.DisplayName;
//import org.mockito.ArgumentCaptor;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import java.io.PrintWriter;
//import java.io.StringWriter;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@DisplayName("ClientHandler Tests")
//class ClientHandlerTest {
//
//    @Mock
//    private UserService mockUserService;
//
//    private StringWriter stringWriter;
//    private PrintWriter output;
//    private ClientHandler clientHandler;
//
//    @BeforeEach
//    void setUp() {
//        MockitoAnnotations.openMocks(this);
//        // Use real PrintWriter with StringWriter to capture output
//        stringWriter = new StringWriter();
//        output = new PrintWriter(stringWriter);
//        clientHandler = new ClientHandler(null, mockUserService);
//    }
//
//    private String getOutput() {
//        output.flush();
//        return stringWriter.toString();
//    }
//
//    @Test
//    @DisplayName("Should process valid REGISTER request successfully")
//    void testProcessValidRegisterRequest() {
//        // Arrange
//        String registerRequest = "REGISTER|newuser|password123|password123|BIDDER";
//        RegisterResponse successResponse = new RegisterResponse(true, "Registration successful");
//
//        when(mockUserService.register(any(RegisterRequest.class))).thenReturn(successResponse);
//
//        // Act
//        clientHandler.processRequest(registerRequest, output);
//
//        // Assert
//        verify(mockUserService).register(any(RegisterRequest.class));
//        assertTrue(getOutput().contains("SUCCESS - Registration successful"));
//    }
//
//    @Test
//    @DisplayName("Should process valid LOGIN request successfully")
//    void testProcessValidLoginRequest() {
//        // Arrange
//        String loginRequest = "LOGIN|testuser|password123";
//        LoginResponse successResponse = new LoginResponse(true, "Login successful", "testuser", "BIDDER");
//
//        when(mockUserService.login(any(LoginRequest.class))).thenReturn(successResponse);
//
//        // Act
//        clientHandler.processRequest(loginRequest, output);
//
//        // Assert
//        verify(mockUserService).login(any(LoginRequest.class));
//        assertTrue(getOutput().contains("SUCCESS - Login successful"));
//    }
//
//    @Test
//    @DisplayName("Should handle failed registration")
//    void testProcessFailedRegisterRequest() {
//        // Arrange
//        String registerRequest = "REGISTER|existinguser|password123|password123|SELLER";
//        RegisterResponse failResponse = new RegisterResponse(false, "User already exists");
//
//        when(mockUserService.register(any(RegisterRequest.class))).thenReturn(failResponse);
//
//        // Act
//        clientHandler.processRequest(registerRequest, output);
//
//        // Assert
//        assertTrue(getOutput().contains("FAIL - User already exists"));
//    }
//
//    @Test
//    @DisplayName("Should handle failed login")
//    void testProcessFailedLoginRequest() {
//        // Arrange
//        String loginRequest = "LOGIN|invaliduser|wrongpass";
//        LoginResponse failResponse = new LoginResponse(false, "Invalid credentials", null, null);
//
//        when(mockUserService.login(any(LoginRequest.class))).thenReturn(failResponse);
//
//        // Act
//        clientHandler.processRequest(loginRequest, output);
//
//        // Assert
//        assertTrue(getOutput().contains("FAIL - Invalid credentials"));
//    }
//
//    @Test
//    @DisplayName("Should reject REGISTER with incorrect number of parts")
//    void testProcessRegisterWithIncorrectPartsCount() {
//        // Arrange - only 4 parts instead of 5
//        String invalidRequest = "REGISTER|user|pass|pass";
//
//        // Act
//        clientHandler.processRequest(invalidRequest, output);
//
//        // Assert
//        verify(mockUserService, never()).register(any());
//        assertTrue(getOutput().contains("FAIL - Invalid request"));
//    }
//
//    @Test
//    @DisplayName("Should reject LOGIN with incorrect number of parts")
//    void testProcessLoginWithIncorrectPartsCount() {
//        // Arrange - 4 parts instead of 3
//        String invalidRequest = "LOGIN|user|pass|extra";
//
//        // Act
//        clientHandler.processRequest(invalidRequest, output);
//
//        // Assert
//        verify(mockUserService, never()).login(any());
//        assertTrue(getOutput().contains("FAIL - Invalid request"));
//    }
//
//    @Test
//    @DisplayName("Should handle unknown command")
//    void testProcessUnknownCommand() {
//        // Arrange
//        String unknownRequest = "DELETE|user";
//
//        // Act
//        clientHandler.processRequest(unknownRequest, output);
//
//        // Assert
//        verify(mockUserService, never()).register(any());
//        verify(mockUserService, never()).login(any());
//        assertTrue(getOutput().contains("FAIL - Invalid request"));
//    }
//
//    @Test
//    @DisplayName("Should handle empty request")
//    void testProcessEmptyRequest() {
//        // Arrange
//        String emptyRequest = "";
//
//        // Act
//        clientHandler.processRequest(emptyRequest, output);
//
//        // Assert
//        assertTrue(getOutput().contains("FAIL - Invalid request format"));
//    }
//
//    @Test
//    @DisplayName("Should handle null request")
//    void testProcessNullRequest() {
//        // Act
//        clientHandler.processRequest(null, output);
//
//        // Assert
//        assertTrue(getOutput().contains("FAIL - Invalid request format"));
//    }
//
//    @Test
//    @DisplayName("Should be case-insensitive for commands")
//    void testProcessCaseInsensitiveCommands() {
//        // Arrange
//        String registerRequest = "register|user|pass|pass|BIDDER";
//        RegisterResponse response = new RegisterResponse(true, "Success");
//
//        when(mockUserService.register(any(RegisterRequest.class))).thenReturn(response);
//
//        // Act
//        clientHandler.processRequest(registerRequest, output);
//
//        // Assert
//        verify(mockUserService).register(any(RegisterRequest.class));
//    }
//
//    @Test
//    @DisplayName("Should handle lowercase login command")
//    void testProcessLowercaseLoginCommand() {
//        // Arrange
//        String loginRequest = "login|user|pass";
//        LoginResponse response = new LoginResponse(true, "Success", "user", "BIDDER");
//
//        when(mockUserService.login(any(LoginRequest.class))).thenReturn(response);
//
//        // Act
//        clientHandler.processRequest(loginRequest, output);
//
//        // Assert
//        verify(mockUserService).login(any(LoginRequest.class));
//    }
//
//    @Test
//    @DisplayName("Should extract correct credentials from REGISTER request")
//    void testExtractRegisterCredentials() {
//        // Arrange
//        String registerRequest = "REGISTER|john_doe|secret123|secret123|ADMIN";
//        RegisterResponse response = new RegisterResponse(true, "Success");
//
//        when(mockUserService.register(any(RegisterRequest.class))).thenReturn(response);
//
//        ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
//
//        // Act
//        clientHandler.processRequest(registerRequest, output);
//
//        // Assert
//        verify(mockUserService).register(captor.capture());
//        RegisterRequest capturedRequest = captor.getValue();
//
//        assertNotNull(capturedRequest);
//    }
//
//    @Test
//    @DisplayName("Should extract correct credentials from LOGIN request")
//    void testExtractLoginCredentials() {
//        // Arrange
//        String loginRequest = "LOGIN|john_doe|secret123";
//        LoginResponse response = new LoginResponse(true, "Success", "john_doe", "ADMIN");
//
//        when(mockUserService.login(any(LoginRequest.class))).thenReturn(response);
//
//        ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
//
//        // Act
//        clientHandler.processRequest(loginRequest, output);
//
//        // Assert
//        verify(mockUserService).login(captor.capture());
//        LoginRequest capturedRequest = captor.getValue();
//
//        assertNotNull(capturedRequest);
//    }
//
//    @Test
//    @DisplayName("Should call userService exactly once for REGISTER")
//    void testUserServiceCalledOnceForRegister() {
//        // Arrange
//        String registerRequest = "REGISTER|user|pass|pass|BIDDER";
//        when(mockUserService.register(any(RegisterRequest.class)))
//            .thenReturn(new RegisterResponse(true, "Success"));
//
//        // Act
//        clientHandler.processRequest(registerRequest, output);
//
//        // Assert
//        verify(mockUserService, times(1)).register(any());
//    }
//
//    @Test
//    @DisplayName("Should call userService exactly once for LOGIN")
//    void testUserServiceCalledOnceForLogin() {
//        // Arrange
//        String loginRequest = "LOGIN|user|pass";
//        when(mockUserService.login(any(LoginRequest.class)))
//            .thenReturn(new LoginResponse(true, "Success", "user", "BIDDER"));
//
//        // Act
//        clientHandler.processRequest(loginRequest, output);
//
//        // Assert
//        verify(mockUserService, times(1)).login(any());
//    }
//
//    @Test
//    @DisplayName("Should handle null message in response")
//    void testProcessRequestWithNullMessage() {
//        // Arrange
//        String loginRequest = "LOGIN|user|pass";
//        LoginResponse response = new LoginResponse(false, null, null, null);
//
//        when(mockUserService.login(any(LoginRequest.class))).thenReturn(response);
//
//        // Act
//        clientHandler.processRequest(loginRequest, output);
//
//        // Assert
//        assertTrue(getOutput().contains("FAIL"));
//    }
//
//    @Test
//    @DisplayName("Should format response correctly with separator")
//    void testResponseFormatWithSeparator() {
//        // Arrange
//        String registerRequest = "REGISTER|user|pass|pass|BIDDER";
//        RegisterResponse response = new RegisterResponse(true, "User created");
//
//        when(mockUserService.register(any(RegisterRequest.class))).thenReturn(response);
//
//        // Act
//        clientHandler.processRequest(registerRequest, output);
//
//        // Assert
//        assertTrue(getOutput().contains("SUCCESS - User created"));
//    }
//
//    @Test
//    @DisplayName("Should handle whitespace in request")
//    void testProcessRequestWithWhitespace() {
//        // Arrange
//        String request = "  ";
//
//        // Act
//        clientHandler.processRequest(request, output);
//
//        // Assert
//        assertTrue(getOutput().contains("FAIL - Invalid request format"));
//    }
//
//    @Test
//    @DisplayName("Should verify REGISTER_PARTS constant")
//    void testRegisterPartsConstant() {
//        assertEquals(5, ClientHandler.REGISTER_PARTS);
//    }
//
//    @Test
//    @DisplayName("Should verify LOGIN_PARTS constant")
//    void testLoginPartsConstant() {
//        assertEquals(3, ClientHandler.LOGIN_PARTS);
//    }
//}
//
