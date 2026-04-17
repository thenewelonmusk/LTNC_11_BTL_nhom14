package com.auction.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class AuctionClient {
    public static void main(String[] args) {
        try(
            Socket socket = new Socket("localhost", 5000);
            Scanner sc = new Scanner(System.in);
            PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            System.out.println("DA KET NOI DEN SERVER DAU GIA");
            boolean running = true;

            while(running){
                String choice = sc.nextLine();

                switch (choice) {
                    case "1":
                        System.out.print("Xu ly dang ky\nNhap username: ");
                        String regUser = sc.nextLine();
                        System.out.print("Nhap mat khau: ");
                        String regPass = sc.nextLine();
                        System.out.print("Xac nhan mat khau: ");
                        String regConf = sc.nextLine();
                        System.out.print("Nhap vai tro BIDDER/SELLER/ADMIN: ");
                        String regRole = sc.nextLine();

                        output.println("REGISTER|" + regUser + "|" + regPass + "|" + regConf + "|" + regRole);
                        System.out.println("Server phản hồi: " + reader.readLine());
                        break;

                    case "2":
                        System.out.print("Xu ly dang nhap\nNhap username: ");
                        String logUser = sc.nextLine();
                        System.out.print("Nhap mat khau: ");
                        String logPass = sc.nextLine();
                        
                        output.println("LOGIN|" + logUser + "|" + logPass);
                        System.out.println("Server phản hồi: " + reader.readLine());
                        break;

                    case "0":
                        running = false;
                        System.out.println("exiting...");
                        break;
                
                    default:
                        System.out.println("LUA CHON KHONG HOP LE");
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("loi ket noi " + e.getMessage());
        }
    }

}
