package com.auction.server;

import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;
import com.auction.dto.RegisterRequest;
import com.auction.dto.RegisterResponse;
import com.auction.service.UserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles client connections to the auction server.
 * Processes registration and login requests.
 */
public class ClientHandler implements Runnable {
    public static final String REGISTER_COMMAND = "REGISTER";
    public static final String LOGIN_COMMAND = "LOGIN";
    public static final String BID_COMMAND = "BID";
    public static final String COMMAND_SEPARATOR = "\\|";
    public static final int REGISTER_PARTS = 5;
    public static final int LOGIN_PARTS = 3;
    public static final int BID_PARTS = 4;
    
    private static final String SUCCESS_RESPONSE = "SUCCESS";
    private static final String FAIL_RESPONSE = "FAIL";
    private static final String RESPONSE_SEPARATOR = " - ";
    
    private final Socket socket;
    private final UserService userService;

    public ClientHandler(Socket socket, UserService userService) {
        this.socket = socket;
        this.userService = userService;
    }

    @Override
    public void run() {
        try (
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String requestLine;
            while ((requestLine = reader.readLine()) != null) {
                processRequest(requestLine, output);
            }
        } catch (IOException e) {
            System.err.println("Handler error: " + e.getMessage());
        }
    }

    /**
     * Processes incoming client requests.
     *
     * @param requestLine the request line from the client
     * @param output the output stream to send responses
     */
    public void processRequest(String requestLine, PrintWriter output) {
        if (requestLine == null || requestLine.isBlank()) {
            sendResponse(output, false, "Invalid request format");
            return;
        }

        String[] parts = requestLine.split(COMMAND_SEPARATOR);
        
        if (parts.length == 0) {
            sendResponse(output, false, "Invalid request format");
            return;
        }

        String action = parts[0].toUpperCase();

        if (REGISTER_COMMAND.equals(action) && parts.length == REGISTER_PARTS) {
            handleRegister(parts, output);
        } else if (LOGIN_COMMAND.equals(action) && parts.length == LOGIN_PARTS) {
            handleLogin(parts, output);
        }
        else {
            sendResponse(output, false, "Invalid request");
        }
    }

    /**
     * Handles user registration requests.
     *
     * @param parts the parsed request parts
     * @param output the output stream to send responses
     */
    private void handleRegister(String[] parts, PrintWriter output) {
        RegisterRequest request = new RegisterRequest(parts[1], parts[2], parts[3], parts[4]);
        RegisterResponse response = userService.register(request);
        sendResponse(output, response.isSuccess(), response.getMessage());
    }

    /**
     * Handles user login requests.
     *
     * @param parts the parsed request parts
     * @param output the output stream to send responses
     */
    private void handleLogin(String[] parts, PrintWriter output) {
        LoginRequest request = new LoginRequest(parts[1], parts[2]);
        LoginResponse response = userService.login(request);
        sendResponse(output, response.isSuccess(), response.getMessage());
    }

    /**
     * Sends a response to the client.
     *
     * @param output the output stream
     * @param success whether the operation was successful
     * @param message the response message
     */
    private void sendResponse(PrintWriter output, boolean success, String message) {
        String status = success ? SUCCESS_RESPONSE : FAIL_RESPONSE;
        String response = status + RESPONSE_SEPARATOR + (message != null ? message : "");
        output.println(response);
    }

}
