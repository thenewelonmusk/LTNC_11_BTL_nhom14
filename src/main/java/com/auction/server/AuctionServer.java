package com.auction.server;

import com.auction.dao.*;
import com.auction.model.Auction;
import com.auction.service.*;
import com.auction.service.impl.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionServer {
	private static final int PORT = 8080;
	// Dùng ThreadPool để tối ưu hiệu suất thay vì new Thread liên tục
	private static final ExecutorService pool = Executors.newFixedThreadPool(5);
	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	public static void main(String[] args) {
		AuctionDAO auctionDAO = new AuctionDAO();
		BidDAO bidDAO = new BidDAO();
		ItemDAO itemDAO = new ItemDAO();
		UserDAO userDAO = new UserDAO();

		AuctionService auctionService = new AuctionServiceImpl(auctionDAO, itemDAO);
		BidService bidService = new BidServiceImpl(bidDAO, auctionDAO, auctionService);
		ItemService itemService = new ItemServiceImpl(itemDAO);
		UserService userService = new UserServiceImpl(userDAO);

		// Thêm shutdown hook để cleanup resources
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			System.out.println("\n[*] Server đang tắt...");
			pool.shutdown();
			scheduler.shutdown();
			DatabaseConnection.closePool();
			System.out.println("[✓] Cleanup hoàn tất");
		}));

		try (ServerSocket serverSocket = new ServerSocket(PORT)) {
			System.out.println("=========================================");
			System.out.println("  SERVER ĐẤU GIÁ ĐANG CHẠY Ở PORT " + PORT);
			System.out.println("  Sẵn sàng nhận kết nối từ Client...");
			System.out.println("=========================================");

			// Kích hoạt luồng theo dõi trạng thái phiên đấu giá real-time
			startAuctionStatusMonitor(auctionService);
			System.out.println("[✓] Luồng kiểm tra trạng thái auction real-time đã bắt đầu");

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
		} finally {
			pool.shutdown();
			scheduler.shutdown();
			DatabaseConnection.closePool();
		}
	}

	private static void startAuctionStatusMonitor(AuctionService auctionService) {
		// Map lưu trạng thái auction trước đó để phát hiện thay đổi
		final java.util.Map<Long, String> previousStatus = new java.util.concurrent.ConcurrentHashMap<>();

		scheduler.scheduleAtFixedRate(() -> {
			try {
				List<Auction> auctionList = auctionService.getAllAuctions();
				for (Auction auction : auctionList) {
					// Trạng thái cũ trước khi refresh
					String oldStatus = previousStatus.getOrDefault(auction.getId(), auction.getStatus().name());

					// Gọi refreshStatus để tự động cập nhật OPEN->RUNNING hoặc RUNNING->FINISHED
					auctionService.refreshStatus(auction.getId());

					// Lấy trạng thái mới sau khi refresh
					Auction updatedAuction = auctionService.getAllAuctions().stream()
							.filter(a -> a.getId().equals(auction.getId())).findFirst().orElse(null);

					if (updatedAuction != null) {
						String newStatus = updatedAuction.getStatus().name();
						// Nếu trạng thái thay đổi, phát broadcast
						if (!oldStatus.equals(newStatus)) {
							System.out
									.println("[*] Auction #" + auction.getId() + ": " + oldStatus + " -> " + newStatus);
							ClientHandler.broadcastStatusUpdate(auction.getId(), newStatus,
									updatedAuction.getEndTime());
							previousStatus.put(auction.getId(), newStatus);
						} else {
							previousStatus.put(auction.getId(), newStatus);
						}
					}
				}
			} catch (Exception e) {
				System.err.println("[-] Lỗi cập nhật trạng thái auction real-time: " + e.getMessage());
			}
		}, 1, 1, TimeUnit.SECONDS);
	}
}