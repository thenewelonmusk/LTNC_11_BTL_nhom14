package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.dto.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.time.LocalDateTime;

public class TestDashboardController {

	@FXML
	private TextField txtUsername, txtPassword, txtItemName, txtStartPrice, txtAuctionId, txtBidAmount;
	@FXML
	private Label lblLoginStatus;
	@FXML
	private ComboBox<String> cbCategory;

	private Long uid = null;
	private Gson gson = new GsonBuilder()
			.registerTypeAdapter(LocalDateTime.class, new com.google.gson.TypeAdapter<LocalDateTime>() {
				@Override
				public void write(com.google.gson.stream.JsonWriter out, LocalDateTime v) throws java.io.IOException {
					out.value(v != null ? v.toString() : null);
				}
				@Override
				public LocalDateTime read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
					return LocalDateTime.parse(in.nextString());
				}
			}).create();

	@FXML
	public void initialize() {
		if (cbCategory != null)
			cbCategory.getSelectionModel().selectFirst();
	}

	@FXML
	public void handleLogin() {
		LoginRequest req = new LoginRequest(txtUsername.getText(), txtPassword.getText());
		String json = NetworkClient.getInstance().sendAndReceive("LOGIN", req);
		if (json != null) {
			LoginResponse res = gson.fromJson(json, LoginResponse.class);
			if (res.isSuccess()) {
				uid = res.getId();
				lblLoginStatus.setText("Đã đăng nhập - ID: " + uid);
				lblLoginStatus.setStyle("-fx-text-fill: green;");
				showAlert("Thông báo", "Đăng nhập thành công!");
			} else {
				showAlert("Lỗi", res.getMessage());
			}
		}
	}

	@FXML
	public void handleRegister() {
		RegisterRequest req = new RegisterRequest(txtUsername.getText(), txtPassword.getText(), txtPassword.getText(),
				"SELLER");
		String json = NetworkClient.getInstance().sendAndReceive("REGISTER", req);
		if (json != null) {
			RegisterResponse res = gson.fromJson(json, RegisterResponse.class);
			showAlert("Kết quả", res.getMessage());
		}
	}

	@FXML
	public void handleCreateAndOpenAuction() {
		if (uid == null) {
			showAlert("Cảnh báo", "Vui lòng đăng nhập trước!");
			return;
		}

		// BƯỚC 1: TẠO SẢN PHẨM (SAVE_ITEM)
		ItemRequest itemReq = new ItemRequest();
		itemReq.setName(txtItemName.getText());
		itemReq.setType(cbCategory.getValue());
		itemReq.setStartingPrice(Double.parseDouble(txtStartPrice.getText()));
		itemReq.setSellerId(uid);

		String resJson1 = NetworkClient.getInstance().sendAndReceive("SAVE_ITEM", itemReq);
		ItemResponse itemRes = gson.fromJson(resJson1, ItemResponse.class);

		if (itemRes != null && itemRes.isSuccess()) {
			Long newItemId = itemRes.getItemData().getItemId();

			if (newItemId == null) {
				showAlert("Lỗi Logic", "Sản phẩm đã tạo nhưng Server không trả về ID. Hãy sửa ItemDAO trả về Long.");
				return;
			}

			// BƯỚC 2: TẠO PHIÊN ĐẤU GIÁ (OPEN_AUCTION)
			AuctionRequest aucReq = new AuctionRequest();
			aucReq.setItemId(newItemId);
			aucReq.setSellerId(uid);
			aucReq.setStartingPrice(itemReq.getStartingPrice());
			aucReq.setStartTime(LocalDateTime.now().plusMinutes(1));
			aucReq.setEndTime(LocalDateTime.now().plusDays(1));

			String resJson2 = NetworkClient.getInstance().sendAndReceive("OPEN_AUCTION", aucReq);
			AuctionResponse aucRes = gson.fromJson(resJson2, AuctionResponse.class);

			if (aucRes != null && aucRes.isSuccess()) {
				showAlert("Thành công", "Đã tạo sản phẩm và mở phiên ID: " + aucRes.getAuction().getId());
				txtAuctionId.setText(aucRes.getAuction().getId().toString());
			} else {
				showAlert("Lỗi",
						"Tạo sản phẩm OK nhưng mở phiên thất bại: " + (aucRes != null ? aucRes.getMessage() : "N/A"));
			}
		} else {
			String msg = (itemRes != null) ? itemRes.getMessage() : "Lỗi kết nối Server";
			showAlert("Lỗi", msg);
		}
	}

	@FXML
	public void handlePlaceBid() {
		if (uid == null)
			return;
		try {
			BidRequest req = new BidRequest();
			req.setAuctionId(Long.parseLong(txtAuctionId.getText()));
			req.setAmount(Double.parseDouble(txtBidAmount.getText()));
			req.setUserId(uid);

			String json = NetworkClient.getInstance().sendAndReceive("PLACE_BID", req);
			BidResponse res = gson.fromJson(json, BidResponse.class);
			showAlert(res.isSuccess() ? "Thành công" : "Thất bại",
					res.getMessage() + "\nGiá hiện tại: " + res.getCurrentPrice());
		} catch (Exception e) {
			showAlert("Lỗi", "Dữ liệu nhập vào không hợp lệ.");
		}
	}

	private void showAlert(String title, String msg) {
		Alert alert = new Alert(title.equals("Lỗi") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(msg);
		alert.show();
	}
}