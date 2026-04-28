package com.auction.client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class AuctionClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;
    private static final String REGISTER_OPTION = "1";
    private static final String LOGIN_OPTION = "2";
    private static final String EXIT_OPTION = "0";
    private static final String REGISTER_COMMAND = "REGISTER";
    private static final String LOGIN_COMMAND = "LOGIN";
    private static final String COMMAND_SEPARATOR = "|";

    public static void main(String[] args) {
        try (
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            Scanner scanner = new Scanner(System.in);
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            System.out.println("Connected to auction server");
            boolean running = true;

            while (running) {
                String choice = scanner.nextLine();

                switch (choice) {
                    case REGISTER_OPTION:
                        handleRegistration(scanner, output, reader);
                        break;

                    case LOGIN_OPTION:
                        handleLogin(scanner, output, reader);
                        break;

                    case EXIT_OPTION:
                        running = false;
                        System.out.println("Exiting...");
                        break;

                    default:
                        System.out.println("Invalid option");
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Connection error: " + e.getMessage());
        }
    }

    private static void handleRegistration(Scanner scanner, PrintWriter output, BufferedReader reader) throws Exception {
        System.out.print("Processing registration\nEnter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();
        System.out.print("Confirm password: ");
        String confirmPassword = scanner.nextLine();
        System.out.print("Enter role BIDDER/SELLER/ADMIN: ");
        String role = scanner.nextLine();

        String message = String.join(COMMAND_SEPARATOR, REGISTER_COMMAND, username, password, confirmPassword, role);
        output.println(message);
        System.out.println("Server response: " + reader.readLine());
    }

    private static void handleLogin(Scanner scanner, PrintWriter output, BufferedReader reader) throws Exception {
        System.out.print("Processing login\nEnter username: ");
        String username = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        String message = String.join(COMMAND_SEPARATOR, LOGIN_COMMAND, username, password);
        output.println(message);
        System.out.println("Server response: " + reader.readLine());
    }
}
