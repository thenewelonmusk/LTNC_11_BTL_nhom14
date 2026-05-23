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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientHandler implements Runnable {

	// DANH SÁCH QUẢN LÝ CÁC CLIENT ĐANG KẾT NỐI (Dùng cho Broadcast Real-time)
	private static final CopyOnWriteArrayList<ClientHandler> activeHandlers = new CopyOnWriteArrayList<>();

	private Socket clientSocket;
	private BufferedReader in;
	private PrintWriter out;
	private Gson gson;

	private UserService userService;
	private ItemService itemService;
	private AuctionService auctionService;
	private BidService bidService;

	private ItemDAO itemDAO;
	private AuctionDAO auctionDAO; // Nâng cấp thành biến toàn cục để dùng trong tính năng Broadcast

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

		UserDAO userDAO = new UserDAO();
		this.itemDAO = new ItemDAO();
		this.auctionDAO = new AuctionDAO(); // Khởi tạo tại đây
		BidDAO bidDAO = new BidDAO();

		this.userService = new UserServiceImpl(userDAO);
		this.itemService = new ItemServiceImpl(itemDAO);
		this.auctionService = new AuctionServiceImpl(this.auctionDAO, itemDAO);

		// Giả sử constructor của BidServiceImpl có hỗ trợ tham số thứ 4 là null cho AutoBidService nếu bạn dùng bản tương thích ngược
		this.bidService = new BidServiceImpl(bidDAO, this.auctionDAO, this.auctionService);

		try {
			in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			out = new PrintWriter(clientSocket.getOutputStream(), true);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Đăng ký luồng xử lý này vào danh sách chung của Server
		activeHandlers.add(this);
	}

	/**
	 * PHƯƠNG THỨC PHÁT TIN BROADCAST (Observer Pattern qua Socket)
	 * Đẩy thông điệp JSON tới tất cả các Client đang bật ứng dụng
	 */
	public static void broadcastUpdate(String updateMessage) {
		for (ClientHandler handler : activeHandlers) {
			try {
				handler.out.println(updateMessage);
			} catch (Exception ignored) {
				// Bỏ qua nếu socket của một client nào đó bị nghẽn
			}
		}
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

					case "PLACE_BID" : {
						BidRequest bReq = gson.fromJson(dataObj, BidRequest.class);
						BidResponse bRes = bidService.placeBid(bReq, bReq.getUserId());
						jsonResponse = gson.toJson(bRes);

						// ĐẢM BẢO AN TOÀN LUỒNG: Chỉ phát tin Broadcast khi giao dịch đặt giá thực sự thành công
						if (bRes != null && bRes.isSuccess() && bRes.getBid() != null) {
							JsonObject broadcastObj = new JsonObject();
							broadcastObj.addProperty("action", "AUCTION_UPDATE");

							JsonObject dataFields = new JsonObject();
							dataFields.addProperty("auctionId", bReq.getAuctionId());
							dataFields.addProperty("currentPrice", bRes.getCurrentPrice());
							dataFields.addProperty("bidderName", "User #" + bReq.getUserId());
							dataFields.addProperty("bidTime", LocalDateTime.now().toString());
							dataFields.addProperty("message", bRes.getMessage() != null ? bRes.getMessage() : "");

							try {
								Auction currentAuction = auctionDAO.findAuction(bReq.getAuctionId());
								if (currentAuction != null && currentAuction.getEndTime() != null) {
									dataFields.addProperty("endTime", currentAuction.getEndTime().toString());
								} else {
									dataFields.addProperty("endTime", LocalDateTime.now().toString());
								}
							} catch(Exception ignored) {
								dataFields.addProperty("endTime", LocalDateTime.now().toString());
							}

							broadcastObj.add("data", dataFields);
							ClientHandler.broadcastUpdate(gson.toJson(broadcastObj));
						}
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
							bo.addProperty("bidderName", "User #" + b.getBidderId());
							bo.addProperty("amount", b.getAmount());
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
				out.println(jsonResponse);
			}
		} catch (Exception e) {
			System.out.println("[-] Client đã ngắt kết nối.");
		} finally {
			// QUAN TRỌNG: Dọn dẹp Observer khi Client ngắt kết nối để giải phóng RAM Server
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
