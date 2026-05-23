package com.auction.client.controller;

import com.auction.client.Session;
import com.auction.client.network.NetworkClient;
import com.auction.dto.BidRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

public class AuctionDetailViewController implements NetworkClient.AuctionUpdateListener {

	@FXML
	private TextField txtAuctionId;
	@FXML
	private Label lblItemName;
	@FXML
	private Label lblItemType;
	@FXML
	private Label lblItemDesc;
	@FXML
	private Label lblStatus;
	@FXML
	private Label lblStart;
	@FXML
	private Label lblCurrent;
	@FXML
	private Label lblEnd;
	@FXML
	private TextField txtBidAmount;
	@FXML
	private Button btnPlaceBid;
	@FXML
	private Label lblBidResult;

	@FXML
	private TableView<BidRow> tblBids;
	@FXML
	private TableColumn<BidRow, String> colBidId;
	@FXML
	private TableColumn<BidRow, String> colBidder;
	@FXML
	private TableColumn<BidRow, Double> colAmount;
	@FXML
	private TableColumn<BidRow, String> colBidTime;

	private Long currentAuctionId;
	private final ObservableList<BidRow> bidsList = FXCollections.observableArrayList();
	private MainWindowController mainWindowController;

	public void setMainWindowController(MainWindowController mainWindowController) {
		this.mainWindowController = mainWindowController;
	}

	@FXML
	public void initialize() {
		if (colBidId != null) {
			colBidId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
		}
		if (colBidder != null) {
			colBidder.setCellValueFactory(cellData -> cellData.getValue().bidderProperty());
		}
		if (colAmount != null) {
			colAmount.setCellValueFactory(cellData -> cellData.getValue().amountProperty().asObject());
		}
		if (colBidTime != null) {
			colBidTime.setCellValueFactory(cellData -> cellData.getValue().timeProperty());
		}
		if (tblBids != null) {
			tblBids.setItems(bidsList);
		}
		NetworkClient.getInstance().addListener(this);
	}

	@FXML
	private void handleLoad() {
		String idText = txtAuctionId != null ? txtAuctionId.getText() : null;
		if (idText == null || idText.trim().isEmpty()) {
			showMessage("Vui lòng nhập ID phiên đấu giá.");
			return;
		}

		try {
			loadAuctionDetail(Long.parseLong(idText.trim()));
		} catch (NumberFormatException e) {
			showMessage("ID phiên đấu giá không hợp lệ.");
		}
	}

	public void loadAuctionDetail(Long auctionId) {
		this.currentAuctionId = auctionId;
		if (txtAuctionId != null) {
			txtAuctionId.setText(String.valueOf(auctionId));
		}

		new Thread(() -> {
			JsonObject dataPayload = new JsonObject();
			dataPayload.addProperty("auctionId", auctionId);
			String jsonResponse = NetworkClient.getInstance().sendAndReceive("GET_AUCTION_DETAIL", dataPayload);

			if (jsonResponse != null) {
				Platform.runLater(() -> parseAndPopulateUI(jsonResponse));
			} else {
				Platform.runLater(() -> showMessage("Không nhận được phản hồi từ máy chủ."));
			}
		}).start();
	}

	private void parseAndPopulateUI(String jsonStr) {
		try {
			JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();
			if (root.has("success") && !root.get("success").getAsBoolean()) {
				showMessage(getStr(root, "message", "Không tải được chi tiết phiên đấu giá."));
				return;
			}

			JsonObject auction = root.has("auction") && root.get("auction").isJsonObject()
					? root.getAsJsonObject("auction")
					: new JsonObject();
			JsonObject item = root.has("item") && root.get("item").isJsonObject()
					? root.getAsJsonObject("item")
					: new JsonObject();
			JsonArray bids = root.has("bids") && root.get("bids").isJsonArray()
					? root.getAsJsonArray("bids")
					: new JsonArray();

			if (lblItemName != null)
				lblItemName.setText(getStr(item, "name", "-"));
			if (lblItemType != null)
				lblItemType.setText(getStr(item, "type", "-"));
			if (lblItemDesc != null)
				lblItemDesc.setText(getStr(item, "description", "-"));
			if (lblStart != null)
				lblStart.setText(formatMoney(getDouble(auction, "startingPrice")));
			if (lblCurrent != null)
				lblCurrent.setText(formatMoney(getDouble(auction, "currentPrice")));
			if (lblEnd != null)
				lblEnd.setText(getStr(auction, "endTime", "-"));
			if (lblStatus != null)
				lblStatus.setText(getStr(auction, "status", "-"));
			if (lblBidResult != null)
				lblBidResult.setText("");

			bidsList.clear();
			int idx = 1;
			for (JsonElement e : bids) {
				JsonObject b = e.getAsJsonObject();
				String bidId = getStr(b, "bidId", getStr(b, "id", String.valueOf(idx++)));
				String bidderName = getStr(b, "bidderName", getStr(b, "bidder", ""));
				double amount = getDouble(b, "amount");
				String bidTime = getStr(b, "bidTime", getStr(b, "time", ""));
				bidsList.add(new BidRow(bidId, bidderName, amount, bidTime));
			}
		} catch (Exception e) {
			showMessage("Lỗi hiển thị dữ liệu phòng.");
		}
	}

	@FXML
	private void handlePlaceBid() {
		if (currentAuctionId == null) {
			showMessage("Chưa có phiên đấu giá để đặt giá.");
			return;
		}
		if (!Session.get().isLoggedIn()) {
			showMessage("Bạn cần đăng nhập trước.");
			return;
		}

		String amountText = txtBidAmount != null ? txtBidAmount.getText().trim() : "";
		if (amountText.isEmpty()) {
			showMessage("Vui lòng nhập số tiền đấu giá.");
			return;
		}

		try {
			double amount = Double.parseDouble(amountText);
			BidRequest req = new BidRequest();
			req.setAuctionId(currentAuctionId);
			req.setAmount(amount);
			req.setUserId(Session.get().getUserId());

			if (btnPlaceBid != null) {
				btnPlaceBid.setDisable(true);
			}

			new Thread(() -> {
				String resStr = NetworkClient.getInstance().sendAndReceive("PLACE_BID", req);
				Platform.runLater(() -> {
					if (btnPlaceBid != null) {
						btnPlaceBid.setDisable(false);
					}

					if (resStr == null || resStr.isBlank()) {
						showMessage("Lỗi: Không nhận được phản hồi từ máy chủ.");
						return;
					}

					try {
						JsonObject resObj = JsonParser.parseString(resStr).getAsJsonObject();
						boolean success = resObj.has("success") && resObj.get("success").getAsBoolean();
						String message = getStr(resObj, "message",
								success ? "Đặt giá thành công!" : "Đặt giá thất bại.");

						if (lblBidResult != null) {
							lblBidResult.setText(message);
						}

						if (success) {
							if (txtBidAmount != null) {
								txtBidAmount.clear();
							}
							if (lblCurrent != null && resObj.has("currentPrice")) {
								lblCurrent.setText(formatMoney(getDouble(resObj, "currentPrice")));
							}
						}
					} catch (Exception ex) {
						showMessage("Lỗi đồng bộ dữ liệu mạng hệ thống.");
					}
				});
			}).start();
		} catch (NumberFormatException ex) {
			showMessage("Số tiền nhập vào không đúng định dạng số hợp lệ.");
		}
	}

	@FXML
	private void handleBack() {
		destroy();
		if (mainWindowController != null) {
			mainWindowController.showBrowseAuctions();
		}
	}

	@Override
	public void onAuctionUpdate(JsonObject data) {
		if (data == null || currentAuctionId == null || !data.has("auctionId")) {
			return;
		}

		if (data.get("auctionId").getAsLong() == currentAuctionId.longValue()) {
			double newPrice = data.has("currentPrice") ? data.get("currentPrice").getAsDouble() : 0.0;
			String newEndTime = getStr(data, "endTime", lblEnd != null ? lblEnd.getText() : "");
			String msg = getStr(data, "message", "Có lượt đặt giá mới!");
			String bidderName = getStr(data, "bidderName", "Đối thủ");
			String bidTime = getStr(data, "bidTime", "Vừa xong");

			Platform.runLater(() -> {
				if (lblCurrent != null) {
					lblCurrent.setText(formatMoney(newPrice));
				}
				if (lblEnd != null) {
					lblEnd.setText(newEndTime);
				}
				bidsList.add(0, new BidRow("", bidderName, newPrice, bidTime));
				if (lblBidResult != null) {
					lblBidResult.setText(msg);
				}
			});
		}
	}

	public void destroy() {
		NetworkClient.getInstance().removeListener(this);
	}

	private void showMessage(String message) {
		if (lblBidResult != null) {
			lblBidResult.setText(message);
		}
	}

	private static String formatMoney(double value) {
		return String.format("%,.0f ₫", value);
	}

	private static String getStr(JsonObject o, String k, String fallback) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : fallback;
	}

	private static double getDouble(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0.0;
	}

	public static class BidRow {
		private final SimpleStringProperty id;
		private final SimpleStringProperty bidder;
		private final SimpleDoubleProperty amount;
		private final SimpleStringProperty time;

		public BidRow(String id, String bidder, double amount, String time) {
			this.id = new SimpleStringProperty(id);
			this.bidder = new SimpleStringProperty(bidder);
			this.amount = new SimpleDoubleProperty(amount);
			this.time = new SimpleStringProperty(time);
		}

		public SimpleStringProperty idProperty() {
			return id;
		}
		public SimpleStringProperty bidderProperty() {
			return bidder;
		}
		public SimpleDoubleProperty amountProperty() {
			return amount;
		}
		public SimpleStringProperty timeProperty() {
			return time;
		}
	}
}
