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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

import java.text.NumberFormat;
import java.util.Locale;

public class MyItemsViewController {

	@FXML
	private TableView<ItemRow> tblItems;
	@FXML
	private TableColumn<ItemRow, String> colId;
	@FXML
	private TableColumn<ItemRow, String> colName;
	@FXML
	private TableColumn<ItemRow, String> colType;
	@FXML
	private TableColumn<ItemRow, String> colPrice;
	@FXML
	private TableColumn<ItemRow, String> colDesc;
	@FXML
	private TableColumn<ItemRow, String> colAction;

	private final ObservableList<ItemRow> data = FXCollections.observableArrayList();
	private final NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

	@FXML
	public void initialize() {
		colId.setCellValueFactory(c -> c.getValue().id);
		colName.setCellValueFactory(c -> c.getValue().name);
		colType.setCellValueFactory(c -> c.getValue().type);
		colPrice.setCellValueFactory(c -> c.getValue().price);
		colDesc.setCellValueFactory(c -> c.getValue().desc);

		colAction.setCellFactory(col -> new TableCell<>() {
			private final Button bEdit = new Button("✏ Sửa");
			private final Button bAuc = new Button("🚀 Mở phiên");
			private final HBox box = new HBox(6, bEdit, bAuc);
			{
				bEdit.getStyleClass().add("btn-ghost");
				bAuc.getStyleClass().add("btn-success");
				bEdit.setOnAction(e -> {
					ItemRow r = getTableView().getItems().get(getIndex());
					try {
						Session.get().setSelectedItemId(Long.parseLong(r.id.get()));
					} catch (Exception ignored) {
					}
					MainWindowController main = MainWindowController.get();
					if (main != null)
						main.showItemForm();
				});
				bAuc.setOnAction(e -> {
					ItemRow r = getTableView().getItems().get(getIndex());
					try {
						Session.get().setSelectedItemId(Long.parseLong(r.id.get()));
					} catch (Exception ignored) {
					}
					MainWindowController main = MainWindowController.get();
					if (main != null)
						main.showOpenAuction();
				});
			}
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty)
					setGraphic(null);
				else
					setGraphic(box);
			}
		});

		tblItems.setItems(data);
		handleReload();
	}

	@FXML
	public void handleReload() {
		data.clear();
		if (!Session.get().isLoggedIn())
			return;

		JsonObject payload = new JsonObject();
		payload.addProperty("sellerId", Session.get().getUserId());
		String json = NetworkClient.getInstance().sendAndReceive("LIST_MY_ITEMS", payload);
		if (json == null)
			return;

		try {
			JsonElement el = JsonParser.parseString(json);
			JsonArray arr = null;
			if (el.isJsonArray())
				arr = el.getAsJsonArray();
			else if (el.isJsonObject() && el.getAsJsonObject().has("items"))
				arr = el.getAsJsonObject().getAsJsonArray("items");
			if (arr == null)
				return;
			for (JsonElement e : arr) {
				JsonObject o = e.getAsJsonObject();
				ItemRow r = new ItemRow();
				r.id.set(getStr(o, "id"));
				r.name.set(getStr(o, "name"));
				r.type.set(getStr(o, "type"));
				r.price.set(fmt.format(getDouble(o, "startingPrice")) + " ₫");
				r.desc.set(getStr(o, "description"));
				data.add(r);
			}
		} catch (Exception ignored) {
		}
	}

	@FXML
	public void handleAddNew() {
		Session.get().setSelectedItemId(null); // form mới
		MainWindowController main = MainWindowController.get();
		if (main != null)
			main.showItemForm();
	}

	private static String getStr(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
	}
	private static double getDouble(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0;
	}

	public static class ItemRow {
		final SimpleStringProperty id = new SimpleStringProperty();
		final SimpleStringProperty name = new SimpleStringProperty();
		final SimpleStringProperty type = new SimpleStringProperty();
		final SimpleStringProperty price = new SimpleStringProperty();
		final SimpleStringProperty desc = new SimpleStringProperty();
	}
}
