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
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.text.NumberFormat;
import java.util.Locale;

public class MyBidsViewController {

	@FXML
	private TableView<MyBidRow> tblMyBids;
	@FXML
	private TableColumn<MyBidRow, String> colId;
	@FXML
	private TableColumn<MyBidRow, String> colAuctionId;
	@FXML
	private TableColumn<MyBidRow, String> colItemName;
	@FXML
	private TableColumn<MyBidRow, String> colAmount;
	@FXML
	private TableColumn<MyBidRow, String> colBidTime;
	@FXML
	private TableColumn<MyBidRow, String> colResult;
	@FXML
	private Label lblEmpty;

	private final ObservableList<MyBidRow> data = FXCollections.observableArrayList();
	private final NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

	@FXML
	public void initialize() {
		colId.setCellValueFactory(c -> c.getValue().id);
		colAuctionId.setCellValueFactory(c -> c.getValue().auctionId);
		colItemName.setCellValueFactory(c -> c.getValue().itemName);
		colAmount.setCellValueFactory(c -> c.getValue().amount);
		colBidTime.setCellValueFactory(c -> c.getValue().bidTime);
		colResult.setCellValueFactory(c -> c.getValue().result);
		tblMyBids.setItems(data);
		handleReload();
	}

	@FXML
	public void handleReload() {
		data.clear();
		if (!Session.get().isLoggedIn()) {
			lblEmpty.setText("Vui lòng đăng nhập để xem lịch sử bid.");
			return;
		}
		JsonObject payload = new JsonObject();
		payload.addProperty("userId", Session.get().getUserId());

		String json = NetworkClient.getInstance().sendAndReceive("LIST_MY_BIDS", payload);
		if (json == null) {
			lblEmpty.setText("Không thể tải dữ liệu từ server.");
			return;
		}

		try {
			JsonElement el = JsonParser.parseString(json);
			JsonArray arr = null;
			if (el.isJsonArray())
				arr = el.getAsJsonArray();
			else if (el.isJsonObject() && el.getAsJsonObject().has("bids"))
				arr = el.getAsJsonObject().getAsJsonArray("bids");

			if (arr == null || arr.size() == 0) {
				lblEmpty.setText("Chưa có lượt đặt giá nào.");
				return;
			}
			lblEmpty.setText("");
			for (JsonElement e : arr) {
				JsonObject o = e.getAsJsonObject();
				MyBidRow r = new MyBidRow();
				r.id.set(getStr(o, "id"));
				r.auctionId.set(getStr(o, "auctionId"));
				r.itemName.set(getStr(o, "itemName"));
				r.amount.set(fmt.format(getDouble(o, "amount")) + " ₫");
				r.bidTime.set(getStr(o, "bidTime"));
				r.result.set(getStr(o, "result"));
				data.add(r);
			}
		} catch (Exception ex) {
			lblEmpty.setText("Server chưa hỗ trợ chức năng này.");
		}
	}

	private static String getStr(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
	}
	private static double getDouble(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0;
	}

	public static class MyBidRow {
		final SimpleStringProperty id = new SimpleStringProperty();
		final SimpleStringProperty auctionId = new SimpleStringProperty();
		final SimpleStringProperty itemName = new SimpleStringProperty();
		final SimpleStringProperty amount = new SimpleStringProperty();
		final SimpleStringProperty bidTime = new SimpleStringProperty();
		final SimpleStringProperty result = new SimpleStringProperty();
	}
}
