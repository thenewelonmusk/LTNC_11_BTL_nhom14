package com.auction.server;

import com.auction.repository.InMemoryUserRepository;
import com.auction.service.UserService;
import com.auction.service.impl.UserServiceImpl;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AuctionServer {
    public static final int SERVER_PORT = 5000;
    
    private final UserService userService;

    public AuctionServer() {
        this.userService = new UserServiceImpl(new InMemoryUserRepository());
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(SERVER_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                new Thread(new ClientHandler(clientSocket, userService)).start();
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new AuctionServer().start();
    }
}