package com.auction.server;

import com.auction.dao.*;
import com.auction.dto.*;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.item.Item;
import com.auction.service.*;
import com.auction.service.impl.*;
import com.google.gson.*;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

	// DANH SÁCH QUẢN LÝ CÁC CLIENT ĐANG KẾT NỐI (Dùng cho Broadcast Real-time)
	private static final CopyOnWriteArrayList<ClientHandler> activeHandlers = new CopyOnWriteArrayList<>();

	// Cache username theo id để khỏi query lại trên mỗi broadcast
	private static final ConcurrentHashMap<Long, String> usernameCache = new ConcurrentHashMap<>();

	private Socket clientSocket;
	private BufferedReader in;
	private PrintWriter out;
	private Gson gson;

	private UserService userService;
	private ItemService itemService;
	private AuctionService auctionService;
	private BidService bidService;
	private AutoBidService autoBidService;

	private UserDAO userDAO;
	private ItemDAO itemDAO;
	private AuctionDAO auctionDAO;

	public ClientHandler(Socket socket) {
		this.clientSocket = socket;

		this.gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
			@Override
			public void write(com.google.gson.stream.JsonWriter out, LocalDateTime value) throws IOException {
				out.value(value != null ? value.toString() : null);
			}
			@Override
			public LocalDateTime read(com.google.gson.stream.JsonReader in) throws IOException {
				return LocalDateTime.parse(in.nextString());
			}
		}).create();

		this.userDAO = new UserDAO();
		this.itemDAO = new ItemDAO();
		this.auctionDAO = new AuctionDAO();
		BidDAO bidDAO = new BidDAO();

		this.userService = new UserServiceImpl(userDAO);
		this.itemService = new ItemServiceImpl(itemDAO);
		this.auctionService = new AuctionServiceImpl(this.auctionDAO, itemDAO);

		// QUAN TRỌNG: BidServiceImpl phải được tạo VỚI autoBidService (constructor 4
		// tham số)
		// để nó có thể share lock và kích hoạt cascade auto-bid. Constructor cũ
		// (3 tham số) bỏ qua auto-bid hoàn toàn -> đó là một nguyên nhân khiến
		// auto-bid trông như không chạy.
		this.autoBidService = new AutoBidServiceImpl(bidDAO, this.auctionDAO);
		this.bidService = new BidServiceImpl(bidDAO, this.auctionDAO, this.auctionService, this.autoBidService);

		try {
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			out = new PrintWriter(clientSocket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		activeHandlers.add(this);
	}

	/**
	 * PHƯƠNG THỨC PHÁT TIN BROADCAST (Observer Pattern qua Socket) Đẩy thông điệp
	 * JSON tới tất cả các Client đang bật ứng dụng.
	 *
	 * SỬA: thêm synchronized trên handler.out để tránh hai broadcast cùng lúc ghi
	 * xen kẽ vào cùng một PrintWriter -> tạo JSON hỏng trên client.
	 */
	public static void broadcastUpdate(String updateMessage) {
		for (ClientHandler handler : activeHandlers) {
			if (handler.out == null)
				continue;
			try {
				synchronized (handler.out) {
					handler.out.println(updateMessage);
				}
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * Tra cứu username (có cache). Trả về fallback "User #id" nếu không tìm thấy.
	 */
	private String resolveUsername(Long userId) {
		if (userId == null)
			return "Đối thủ";
		String cached = usernameCache.get(userId);
		if (cached != null)
			return cached;
		String fromDb = userDAO.findUsernameById(userId);
		String name = fromDb != null ? fromDb : ("User #" + userId);
		usernameCache.put(userId, name);
		return name;
	}

	/**
	 * Đóng gói broadcast cho một bid (manual hoặc auto).
	 */
	private void broadcastBid(Long auctionId, BidTransaction bid, boolean isAuto, String overrideMessage) {
		JsonObject broadcastObj = new JsonObject();
		broadcastObj.addProperty("action", "AUCTION_UPDATE");

		JsonObject dataFields = new JsonObject();
		dataFields.addProperty("auctionId", auctionId);
		if (bid != null) {
			dataFields.addProperty("bidId", bid.getId());
			dataFields.addProperty("currentPrice", bid.getAmount());
			dataFields.addProperty("bidderId", bid.getBidderId());
			dataFields.addProperty("bidderName", resolveUsername(bid.getBidderId()));
			dataFields.addProperty("bidTime",
					(bid.getBidTime() != null ? bid.getBidTime() : LocalDateTime.now()).toString());
		} else {
			dataFields.addProperty("bidTime", LocalDateTime.now().toString());
		}
		dataFields.addProperty("autoBid", isAuto);
		dataFields.addProperty("message",
				overrideMessage != null
						? overrideMessage
						: (isAuto ? "Auto-bid vừa đẩy giá lên!" : "Có lượt đặt giá mới!"));

		// Lấy endTime mới nhất từ DB (đã được persist nhờ AuctionDAO.updateAuction đã
		// sửa).
		try {
			Auction currentAuction = auctionDAO.findAuction(auctionId);
			if (currentAuction != null) {
				if (currentAuction.getEndTime() != null) {
					dataFields.addProperty("endTime", currentAuction.getEndTime().toString());
				}
				dataFields.addProperty("status",
						currentAuction.getStatus() != null ? currentAuction.getStatus().name() : "");
				// Nếu bid==null (ví dụ không có bid manual mà cascade khởi nguồn), vẫn dùng
				// currentPrice
				if (bid == null) {
					dataFields.addProperty("currentPrice", currentAuction.getCurrentPrice());
				}
			}
		} catch (Exception ignored) {
			dataFields.addProperty("endTime", LocalDateTime.now().toString());
		}

		broadcastObj.add("data", dataFields);
		broadcastUpdate(gson.toJson(broadcastObj));
	}

	@Override
	public void run() {
		try {
			String clientMessage;
			while ((clientMessage = in.readLine()) != null) {
				System.out.println("[Request từ Client]: " + clientMessage);

				JsonObject requestObj = JsonParser.parseString(clientMessage).getAsJsonObject();
				String action = requestObj.get("action").getAsString();
				JsonObject dataObj = requestObj.getAsJsonObject("data");

				String jsonResponse;

				switch (action) {
					case "LOGIN" : {
						LoginRequest loginReq = gson.fromJson(dataObj, LoginRequest.class);
						LoginResponse loginRes = userService.login(loginReq);
						jsonResponse = gson.toJson(loginRes);
						break;
					}
					case "REGISTER" : {
						RegisterRequest regReq = gson.fromJson(dataObj, RegisterRequest.class);
						RegisterResponse regRes = userService.register(regReq);
						jsonResponse = gson.toJson(regRes);
						break;
					}
					case "SAVE_ITEM" : {
						ItemRequest itemReq = gson.fromJson(dataObj, ItemRequest.class);
						ItemResponse itemRes;
						if (itemReq.getItemId() != null && itemReq.getItemId() > 0) {
							itemRes = itemService.updateItem(itemReq.getItemId(), itemReq, itemReq.getSellerId());
						} else {
							itemRes = itemService.createItem(itemReq, itemReq.getSellerId());
						}
						jsonResponse = gson.toJson(itemRes);
						break;
					}
					case "OPEN_AUCTION" : {
						AuctionRequest aReq = gson.fromJson(dataObj, AuctionRequest.class);
						AuctionResponse aRes = auctionService.openAuction(aReq, aReq.getSellerId());
						jsonResponse = gson.toJson(aRes);
						break;
					}

					case "PLACE_BID": {
						BidRequest bReq = gson.fromJson(dataObj, BidRequest.class);

						// 🌟 BÍ QUYẾT 1: Tạo Hàng đợi (Queue) gom các lượt Auto-bid
						// Gom các lượt nảy số của Bot lại để tránh việc gói tin bay về UI sớm hơn cả người thật
						List<BidTransaction> pendingAutoBids = new ArrayList<>();

						AutoBidService.AutoBidListener listener = (autoBid, ignored) -> {
							pendingAutoBids.add(autoBid);
						};

						// 🌟 BÍ QUYẾT 2: Thực thi nghiệp vụ đặt giá cốt lõi
						BidResponse bRes = bidService.placeBid(bReq, bReq.getUserId(), listener);

						// Gói kết quả để trả về cho Client vừa bấm nút (ẩn loading, báo thành công...)
						jsonResponse = gson.toJson(bRes);

						// 🌟 BÍ QUYẾT 3: Phát tin (Broadcast) chuẩn thứ tự và an toàn
						if (bRes != null && bRes.isSuccess() && bRes.getBid() != null) {
							BidTransaction manualBid = bRes.getBid();

							// (Tuỳ chọn) Nếu bạn muốn bắt sự kiện Anti-sniping gia hạn thêm giờ
        /*
        try {
            Auction currentAuction = auctionDAO.findAuction(bReq.getAuctionId());
            // Lấy currentAuction.getEndTime() gửi kèm nếu UI của bạn cần cập nhật đồng hồ
        } catch (Exception ignored) {}
        */

							// Bước 1: Phát lệnh Broadcast giá của NGƯỜI THẬT trước
							broadcastBid(bReq.getAuctionId(), manualBid, false, bRes.getMessage());

							// Bước 2: Xả hàng đợi, phát lệnh Broadcast giá của BOT đè lên sau
							for (BidTransaction botBid : pendingAutoBids) {

								// 💡 Mẹo UI ăn điểm 10: Thêm delay nhỏ (200ms) giữa các lần bắn
								// Giúp màn hình JavaFX của Client kịp render hiệu ứng nảy số lần lượt (như Shopee/eBay)
								// thay vì nhảy cái rụp qua 10 giá.
								try {
									Thread.sleep(200);
								} catch (InterruptedException ignored) {}

								broadcastBid(bReq.getAuctionId(), botBid, true, "Auto-bid vừa tự động nâng giá!");
							}
						}
						break;
					}

					case "REGISTER_AUTO_BID" : {
						AutoBidRequest req = gson.fromJson(dataObj, AutoBidRequest.class);

						// Listener phát từng auto-bid -> các client khác thấy cascade.
						AutoBidService.AutoBidListener listener = (autoBid, ignored) -> broadcastBid(req.getAuctionId(),
								autoBid, true, null);

						// Đi qua BidService để dùng chung lock với placeBid
						AutoBidResponse res = bidService.registerAutoBidWithLock(req, listener);
						jsonResponse = gson.toJson(res);
						break;
					}

					case "CANCEL_AUTO_BID" : {
						Long auctionId = dataObj.get("auctionId").getAsLong();
						Long bidderId = dataObj.get("bidderId").getAsLong();
						AutoBidResponse res = autoBidService.cancelAutoBid(auctionId, bidderId);
						jsonResponse = gson.toJson(res);
						break;
					}

					// ===== Các action hỗ trợ giao diện =====

					case "LIST_AUCTIONS" : {
						jsonResponse = buildAuctionListJson(auctionService.getAllAuctions());
						break;
					}

					case "LIST_MY_AUCTIONS" : {
						Long sellerId = dataObj.has("sellerId") ? dataObj.get("sellerId").getAsLong() : null;
						if (sellerId == null) {
							jsonResponse = "{\"success\":false, \"message\":\"Thiếu sellerId\"}";
						} else {
							jsonResponse = buildAuctionListJson(auctionService.getAuctionsBySeller(sellerId));
						}
						break;
					}

					case "LIST_MY_ITEMS" : {
						Long sellerId = dataObj.has("sellerId") ? dataObj.get("sellerId").getAsLong() : null;
						if (sellerId == null) {
							jsonResponse = "{\"success\":false, \"message\":\"Thiếu sellerId\"}";
						} else {
							List<Item> items = itemDAO.findBySeller(sellerId);
							JsonArray arr = new JsonArray();
							for (Item it : items) {
								JsonObject o = new JsonObject();
								o.addProperty("id", it.getId());
								o.addProperty("name", it.getName());
								o.addProperty("description", it.getDescription());
								o.addProperty("type", it.getType());
								o.addProperty("startingPrice", it.getStartingPrice());
								arr.add(o);
							}
							JsonObject root = new JsonObject();
							root.add("items", arr);
							jsonResponse = gson.toJson(root);
						}
						break;
					}

					case "LIST_MY_BIDS" : {
						Long userId = dataObj.has("userId") ? dataObj.get("userId").getAsLong() : null;
						if (userId == null) {
							jsonResponse = "{\"success\":false, \"message\":\"Thiếu userId\"}";
						} else {
							List<BidTransaction> bids = bidService.getBidsByBidder(userId);
							JsonArray arr = new JsonArray();
							for (BidTransaction b : bids) {
								JsonObject o = new JsonObject();
								o.addProperty("id", b.getId());
								o.addProperty("auctionId", b.getAuctionId());
								o.addProperty("amount", b.getAmount());
								o.addProperty("bidTime", b.getBidTime() != null ? b.getBidTime().toString() : "");

								String itemName = "";
								try {
									AuctionResponse ar = auctionService.findAuctionById(b.getAuctionId());
									if (ar != null && ar.isSuccess() && ar.getAuction() != null) {
										Item it = itemDAO.findItem(ar.getAuction().getItemId());
										itemName = it.getName();
									}
								} catch (Exception ignored) {
								}
								o.addProperty("itemName", itemName);
								o.addProperty("result", b.isAutoBid() ? "AUTO" : "");
								o.addProperty("autoBid", b.isAutoBid());
								arr.add(o);
							}
							JsonObject root = new JsonObject();
							root.add("bids", arr);
							jsonResponse = gson.toJson(root);
						}
						break;
					}

					case "GET_AUCTION_DETAIL" : {
						Long auctionId = dataObj.has("auctionId") ? dataObj.get("auctionId").getAsLong() : null;
						if (auctionId == null) {
							jsonResponse = "{\"success\":false, \"message\":\"Thiếu auctionId\"}";
							break;
						}
						AuctionResponse ar = auctionService.findAuctionById(auctionId);
						if (ar == null || !ar.isSuccess() || ar.getAuction() == null) {
							jsonResponse = "{\"success\":false, \"message\":\"Không tìm thấy phiên đấu giá\"}";
							break;
						}
						Auction a = ar.getAuction();
						JsonObject auctionJson = new JsonObject();
						auctionJson.addProperty("id", a.getId());
						auctionJson.addProperty("startingPrice", a.getStartingPrice());
						auctionJson.addProperty("currentPrice", a.getCurrentPrice());
						auctionJson.addProperty("startTime",
								a.getStartTime() != null ? a.getStartTime().toString() : "");
						auctionJson.addProperty("endTime", a.getEndTime() != null ? a.getEndTime().toString() : "");
						auctionJson.addProperty("status", a.getStatus() != null ? a.getStatus().name() : "");

						JsonObject itemJson = new JsonObject();
						try {
							Item it = itemDAO.findItem(a.getItemId());
							itemJson.addProperty("id", it.getId());
							itemJson.addProperty("name", it.getName());
							itemJson.addProperty("description", it.getDescription());
							itemJson.addProperty("type", it.getType());
						} catch (Exception ex) {
							itemJson.addProperty("name", "(không tải được)");
						}

						JsonArray bidsArr = new JsonArray();
						for (BidTransaction b : bidService.getBidsByAuction(auctionId)) {
							JsonObject bo = new JsonObject();
							bo.addProperty("id", b.getId());
							bo.addProperty("bidderId", b.getBidderId());
							bo.addProperty("bidderName", resolveUsername(b.getBidderId()));
							bo.addProperty("amount", b.getAmount());
							bo.addProperty("autoBid", b.isAutoBid());
							bo.addProperty("bidTime", b.getBidTime() != null ? b.getBidTime().toString() : "");
							bidsArr.add(bo);
						}

						JsonObject root = new JsonObject();
						root.add("auction", auctionJson);
						root.add("item", itemJson);
						root.add("bids", bidsArr);
						jsonResponse = gson.toJson(root);
						break;
					}

					default :
						jsonResponse = "{\"success\":false, \"message\":\"Hành động không hợp lệ!\"}";
						break;
				}

				// Tránh xen kẽ với broadcast trên cùng PrintWriter
				synchronized (out) {
					out.println(jsonResponse);
				}
			}
		} catch (Exception e) {
			System.out.println("[-] Client đã ngắt kết nối.");
		} finally {
			activeHandlers.remove(this);
			try {
				if (clientSocket != null)
					clientSocket.close();
			} catch (IOException e) {
			}
		}
	}

	private String buildAuctionListJson(List<Auction> auctions) {
		JsonArray arr = new JsonArray();
		if (auctions != null) {
			for (Auction a : auctions) {
				JsonObject o = new JsonObject();
				o.addProperty("id", a.getId());
				o.addProperty("startingPrice", a.getStartingPrice());
				o.addProperty("currentPrice", a.getCurrentPrice());
				o.addProperty("startTime", a.getStartTime() != null ? a.getStartTime().toString() : "");
				o.addProperty("endTime", a.getEndTime() != null ? a.getEndTime().toString() : "");
				o.addProperty("status", a.getStatus() != null ? a.getStatus().name() : "");
				String itemName = "";
				try {
					Item it = itemDAO.findItem(a.getItemId());
					if (it != null)
						itemName = it.getName();
				} catch (Exception ignored) {
				}
				o.addProperty("itemName", itemName);
				arr.add(o);
			}
		}
		JsonObject root = new JsonObject();
		root.add("auctions", arr);
		return gson.toJson(root);
	}
}
