package com.auction.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
	private static final int PORT = 8080;
	// Dùng ThreadPool để tối ưu hiệu suất thay vì new Thread liên tục
	private static final ExecutorService pool = Executors.newFixedThreadPool(50);

	public static void main(String[] args) {
		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("=========================================");
			System.out.println("  SERVER ĐẤU GIÁ ĐANG CHẠY Ở PORT " + PORT);
			System.out.println("  Sẵn sàng nhận kết nối từ Client...");
			System.out.println("=========================================");

			while (true) {
				// Chặn ở đây chờ Client kết nối tới
				Socket clientSocket = serverSocket.accept();
				System.out.println("[+] Client mới kết nối: " + clientSocket.getInetAddress());

				// Ném Client cho ClientHandler xử lý trên một luồng riêng
				ClientHandler clientThread = new ClientHandler(clientSocket);
				pool.execute(clientThread);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}