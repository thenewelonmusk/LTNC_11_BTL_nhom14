package com.auction.client.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

public class NetworkClient {
	private static NetworkClient instance;
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;

	// SỬA LỖI: Trang bị TypeAdapter cho LocalDateTime để Gson không dùng Reflection
	private final Gson gson = new GsonBuilder()
			.registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
				@Override
				public void write(JsonWriter out, LocalDateTime value) throws IOException {
					out.value(value != null ? value.toString() : null);
				}
				@Override
				public LocalDateTime read(JsonReader in) throws IOException {
					return LocalDateTime.parse(in.nextString());
				}
			}).create();

	private NetworkClient() {
		connect();
	}

	public static NetworkClient getInstance() {
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
			System.out.println("Đã kết nối tới Server thành công!");
		} catch (IOException e) {
			System.err.println("LỖI: Không thể kết nối đến Server. Vui lòng bật Server trước!");
			socket = null;
		}
	}

	public String sendAndReceive(String action, Object dataPayload) {
		if (socket == null || out == null || in == null) {
			System.err.println("Chưa có kết nối tới Server.");
			return null;
		}

		try {
			// Gson giờ đã biết cách xử lý LocalDateTime một cách an toàn
			String jsonData = gson.toJson(dataPayload);
			String requestMessage = "{\"action\":\"" + action + "\", \"data\":" + jsonData + "}";

			out.println(requestMessage);
			return in.readLine();

		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}