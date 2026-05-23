package com.auction.client.controller;

import com.auction.client.Session;
import com.auction.client.network.NetworkClient;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

/**
 * View danh sách phiên đấu giá cho Bidder.
 *
 * Gọi action "LIST_AUCTIONS" tới server (nếu server có hỗ trợ). Nếu server chưa
 * hỗ trợ, sẽ hiển thị bảng rỗng + thông báo.
 */
public class BrowseAuctionsViewController {

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

	@FXML
	public void initialize() {
		colId.setCellValueFactory(c -> c.getValue().idProp);
		colItemName.setCellValueFactory(c -> c.getValue().nameProp);
		colStart.setCellValueFactory(c -> c.getValue().startProp);
		colCurrent.setCellValueFactory(c -> c.getValue().currentProp);
		colEndTime.setCellValueFactory(c -> c.getValue().endProp);
		colStatus.setCellValueFactory(c -> c.getValue().statusProp);

		// Status pill renderer
		colStatus.setCellFactory(col -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || item == null) {
					setText(null);
					setGraphic(null);
					return;
				}
				Label l = new Label(item);
				String s = item.toUpperCase();
				if (s.contains("OPEN"))
					l.getStyleClass().add("status-open");
				else if (s.contains("CLOSE"))
					l.getStyleClass().add("status-closed");
				else
					l.getStyleClass().add("status-end");
				setGraphic(l);
				setText(null);
			}
		});

		// "Đặt giá" button per row
		colAction.setCellFactory(col -> new TableCell<>() {
			private final Button btn = new Button("🎯 Đặt giá");
			{
				btn.getStyleClass().add("btn-primary");
				btn.setOnAction(e -> {
					AuctionRow row = getTableView().getItems().get(getIndex());
					try {
						Session.get().setSelectedAuctionId(Long.parseLong(row.idProp.get()));
					} catch (Exception ignored) {
					}
					MainWindowController main = MainWindowController.get();
					if (main != null)
						main.showAuctionDetail();
				});
			}
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty)
					setGraphic(null);
				else
					setGraphic(btn);
			}
		});

		tblAuctions.setItems(data);
		loadAuctions(null);
	}

	@FXML
	public void handleReload() {
		loadAuctions(null);
	}

	@FXML
	public void handleSearch() {
		String q = txtSearch.getText();
		loadAuctions(q == null ? "" : q.trim().toLowerCase());
	}

	/**
	 * Gọi server "LIST_AUCTIONS". Server hiện chưa có action này thì ta vẫn xử lý
	 * fallback gracefully.
	 */
	private void loadAuctions(String filter) {
		data.clear();
		String json = NetworkClient.getInstance().sendAndReceive("LIST_AUCTIONS", new Object());
		if (json == null || json.equals("null") || json.isBlank())
			return;

		try {
			JsonElement el = JsonParser.parseString(json);
			// Nếu server trả về "Hành động không hợp lệ!" thì dừng lặng lẽ
			if (el.isJsonObject() && el.getAsJsonObject().has("success")
					&& !el.getAsJsonObject().get("success").getAsBoolean()) {
				return;
			}
			JsonArray arr = null;
			if (el.isJsonArray())
				arr = el.getAsJsonArray();
			else if (el.isJsonObject() && el.getAsJsonObject().has("auctions"))
				arr = el.getAsJsonObject().getAsJsonArray("auctions");
			if (arr == null)
				return;

			NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
			for (JsonElement e : arr) {
				JsonObject o = e.getAsJsonObject();
				AuctionRow r = new AuctionRow();
				r.idProp.set(getStr(o, "id"));
				r.nameProp.set(getStr(o, "itemName"));
				r.startProp.set(fmt.format(getDouble(o, "startingPrice")) + " ₫");
				r.currentProp.set(fmt.format(getDouble(o, "currentPrice")) + " ₫");
				r.endProp.set(getStr(o, "endTime"));
				r.statusProp.set(getStr(o, "status"));
				if (filter == null || filter.isEmpty() || r.nameProp.get().toLowerCase().contains(filter)
						|| r.idProp.get().contains(filter)) {
					data.add(r);
				}
			}
		} catch (Exception ignored) {
			/* server chưa hỗ trợ – bỏ qua */ }
	}

	private static String getStr(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
	}
	private static double getDouble(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0;
	}

	/** Row model. */
	public static class AuctionRow {
		final SimpleStringProperty idProp = new SimpleStringProperty();
		final SimpleStringProperty nameProp = new SimpleStringProperty();
		final SimpleStringProperty startProp = new SimpleStringProperty();
		final SimpleStringProperty currentProp = new SimpleStringProperty();
		final SimpleStringProperty endProp = new SimpleStringProperty();
		final SimpleStringProperty statusProp = new SimpleStringProperty();
	}
}
