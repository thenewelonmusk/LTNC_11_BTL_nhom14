package com.auction.client.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Exchanger;

public class NetworkClient {
	private static NetworkClient instance;
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private final Gson gson;

	// Bộ trao đổi dữ liệu giúp luồng chính nhận đúng gói phản hồi từ luồng nghe nền
	private final Exchanger<String> responseExchanger = new Exchanger<>();

	// Danh sách các lớp giao diện đăng ký làm Observer
	private final CopyOnWriteArrayList<AuctionUpdateListener> listeners = new CopyOnWriteArrayList<>();

	public interface AuctionUpdateListener {
		void onAuctionUpdate(JsonObject data);
	}

	private NetworkClient() {
		this.gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
			@Override
			public void write(JsonWriter out, LocalDateTime value) throws IOException {
				out.value(value != null ? value.toString() : null);
			}
			@Override
			public LocalDateTime read(JsonReader in) throws IOException {
				return LocalDateTime.parse(in.nextString());
			}
		}).create();
		connect();
	}

	public static synchronized NetworkClient getInstance() {
		if (instance == null) {
			instance = new NetworkClient();
		}
		return instance;
	}

	public void connect() {
		try {
			socket = new Socket("localhost", 8080);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			System.out.println("[Network] Kết nối đến Server thành công!");

			// DUY NHẤT luồng này được phép đọc từ socket.getInputStream()
			startListening();
		} catch (IOException e) {
			System.err.println("[Network] LỖI: Không thể kết nối tới Server.");
			socket = null;
		}
	}

	/**
	 * Luồng độc quyền đọc dữ liệu từ Socket Stream để tránh xung đột
	 */
	private void startListening() {
		new Thread(() -> {
			try {
				String line;
				while ((line = in.readLine()) != null) {
					try {
						JsonObject json = JsonParser.parseString(line).getAsJsonObject();

						// Kiểm tra nếu là tin nhắn đẩy Real-time dạng Broadcast
						if (json.has("action") && "AUCTION_UPDATE".equals(json.get("action").getAsString())) {
							JsonObject data = json.getAsJsonObject("data");
							for (AuctionUpdateListener listener : listeners) {
								listener.onAuctionUpdate(data);
							}
						} else {
							// Nếu là gói phản hồi Request-Response thông thường -> ném qua Exchanger cho
							// luồng đang đợi
							responseExchanger.exchange(line);
						}
					} catch (Exception e) {
						System.err.println("Lỗi xử lý gói tin: " + e.getMessage());
					}
				}
			} catch (IOException e) {
				System.err.println("Mất kết nối dòng mạng từ Server.");
			}
		}).start();
	}

	public void addListener(AuctionUpdateListener listener) {
		listeners.add(listener);
	}

	public void removeListener(AuctionUpdateListener listener) {
		listeners.remove(listener);
	}

	public String sendAndReceive(String action, Object dataPayload) {
		if (socket == null || out == null) {
			return "{\"success\":false, \"message\":\"Chưa kết nối mạng!\"}";
		}

		try {
			String jsonData = gson.toJson(dataPayload);
			String requestMessage = "{\"action\":\"" + action + "\", \"data\":" + jsonData + "}";

			// Gửi dữ liệu lên Server
			out.println(requestMessage);

			// Khóa chặn luồng và đứng đợi luồng ngầm startListening đẩy dữ liệu phản hồi về
			// qua đây
			return responseExchanger.exchange(null);

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return "{\"success\":false, \"message\":\"Yêu cầu bị gián đoạn!\"}";
		}
	}

}