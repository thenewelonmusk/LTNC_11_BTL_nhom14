package com.auction.client.controller;

import com.auction.client.Session;
import com.auction.client.network.NetworkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.text.NumberFormat;
import java.util.Locale;

public class BrowseAuctionsViewController implements NetworkClient.AuctionUpdateListener {

	@FXML
	private TableView<AuctionRow> tblAuctions;
	@FXML
	private TableColumn<AuctionRow, String> colId;
	@FXML
	private TableColumn<AuctionRow, String> colItemName;
	@FXML
	private TableColumn<AuctionRow, String> colStart;
	@FXML
	private TableColumn<AuctionRow, String> colCurrent;
	@FXML
	private TableColumn<AuctionRow, String> colEndTime;
	@FXML
	private TableColumn<AuctionRow, String> colStatus;
	@FXML
	private TableColumn<AuctionRow, String> colAction;
	@FXML
	private TextField txtSearch;

	private final ObservableList<AuctionRow> data = FXCollections.observableArrayList();
	private final NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

	@FXML
	public void initialize() {
		colId.setCellValueFactory(c -> c.getValue().id);
		colItemName.setCellValueFactory(c -> c.getValue().name);
		colStart.setCellValueFactory(c -> c.getValue().start);
		colCurrent.setCellValueFactory(c -> c.getValue().current);
		colEndTime.setCellValueFactory(c -> c.getValue().endTime);
		colStatus.setCellValueFactory(c -> c.getValue().status);

		colStatus.setCellFactory(col -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setGraphic(null);
					setText(null);
					return;
				}
				Label l = new Label(item);
				String s = item.toUpperCase(Locale.ROOT);
				if (s.contains("OPEN")) {
					l.getStyleClass().add("status-open");
				} else if (s.contains("CLOSE")) {
					l.getStyleClass().add("status-closed");
				} else {
					l.getStyleClass().add("status-end");
				}
				setGraphic(l);
				setText(null);
			}
		});

		colAction.setCellFactory(col -> new TableCell<>() {
			private final Button btn = new Button("👁 Xem");
			{
				btn.getStyleClass().add("btn-ghost");
				btn.setOnAction(e -> {
					AuctionRow r = getTableView().getItems().get(getIndex());
					MainWindowController main = MainWindowController.get();
					if (main != null) {
						Session.get().setSelectedAuctionId(parseLongOrNull(r.id.get()));
						main.showAuctionDetail(parseLongOrNull(r.id.get()));
					}
				});
			}
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				setGraphic(empty ? null : btn);
				setText(null);
			}
		});

		tblAuctions.setItems(data);
		handleReload();

		// Đăng ký nhận cập nhật real-time từ Server
		NetworkClient.getInstance().addListener(this);
	}

	@FXML
	public void handleReload() {
		loadAuctions(null);
	}

	@FXML
	public void handleSearch() {
		String q = txtSearch.getText();
		loadAuctions(q == null ? "" : q.trim().toLowerCase(Locale.ROOT));
	}

	private void loadAuctions(String filter) {
		data.clear();
		String json = NetworkClient.getInstance().sendAndReceive("LIST_AUCTIONS", new Object());
		if (json == null || json.isBlank() || "null".equals(json)) {
			return;
		}

		try {
			JsonElement el = JsonParser.parseString(json);
			if (el.isJsonObject() && el.getAsJsonObject().has("success")
					&& !el.getAsJsonObject().get("success").getAsBoolean()) {
				return;
			}

			JsonArray arr = null;
			if (el.isJsonArray()) {
				arr = el.getAsJsonArray();
			} else if (el.isJsonObject() && el.getAsJsonObject().has("auctions")) {
				arr = el.getAsJsonObject().getAsJsonArray("auctions");
			}
			if (arr == null) {
				return;
			}

			for (JsonElement e : arr) {
				JsonObject o = e.getAsJsonObject();
				AuctionRow r = new AuctionRow();
				r.id.set(getStr(o, "id"));
				r.name.set(getStr(o, "itemName"));
				r.start.set(fmt.format(getDouble(o, "startingPrice")) + " ₫");
				r.current.set(fmt.format(getDouble(o, "currentPrice")) + " ₫");
				r.endTime.set(getStr(o, "endTime"));
				r.status.set(getStr(o, "status"));

				if (filter == null || filter.isEmpty() || r.name.get().toLowerCase(Locale.ROOT).contains(filter)
						|| r.id.get().contains(filter)) {
					data.add(r);
				}
			}
		} catch (Exception ignored) {
		}
	}

	private static String getStr(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
	}

	private static double getDouble(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0;
	}

	private static Long parseLongOrNull(String value) {
		try {
			return value == null || value.isBlank() ? null : Long.parseLong(value.trim());
		} catch (Exception e) {
			return null;
		}
	}

	public static class AuctionRow {
		final SimpleStringProperty id = new SimpleStringProperty();
		final SimpleStringProperty name = new SimpleStringProperty();
		final SimpleStringProperty start = new SimpleStringProperty();
		final SimpleStringProperty current = new SimpleStringProperty();
		final SimpleStringProperty endTime = new SimpleStringProperty();
		final SimpleStringProperty status = new SimpleStringProperty();

		public Long getId() {
			try {
				return Long.parseLong(id.get());
			} catch (Exception e) {
				return null;
			}
		}
	}

	/**
	 * Callback được gọi khi nhận thông điệp cập nhật trạng thái từ Server. Cập nhật
	 * lại hàng tương ứng trong bảng auction list.
	 */
	@Override
	public void onAuctionUpdate(JsonObject data) {
		// Chạy trên FX Application Thread để an toàn
		Platform.runLater(() -> {
			try {
				if (!data.has("auctionId")) {
					return;
				}

				Long auctionId = data.get("auctionId").getAsLong();
				String newStatus = data.has("status") ? data.get("status").getAsString() : null;

				// Tìm row tương ứng trong danh sách hiện tại
				for (AuctionRow row : this.data) {
					if (row.getId() != null && row.getId().equals(auctionId)) {
						// Cập nhật trạng thái nếu có
						if (newStatus != null && !newStatus.isEmpty()) {
							row.status.set(newStatus);
						}
						// Cập nhật endTime nếu có
						if (data.has("endTime") && !data.get("endTime").isJsonNull()) {
							row.endTime.set(data.get("endTime").getAsString());
						}
						System.out.println("[BrowseAuctionsViewController] Cập nhật trạng thái Auction #" + auctionId
								+ " -> " + newStatus);
						break;
					}
				}
			} catch (Exception e) {
				System.err.println("[BrowseAuctionsViewController] Lỗi xử lý cập nhật: " + e.getMessage());
			}
		});
	}
}
