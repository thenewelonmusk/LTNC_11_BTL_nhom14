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

import java.text.NumberFormat;
import java.util.Locale;

public class MyAuctionsViewController {

    @FXML private TableView<AuctionRow> tblAuctions;
    @FXML private TableColumn<AuctionRow, String> colId, colItemName, colStart, colCurrent,
            colStartTime, colEndTime, colStatus, colAction;

    private final ObservableList<AuctionRow> data = FXCollections.observableArrayList();
    private final NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));

    @FXML
    public void initialize() {
        colId.setCellValueFactory(c -> c.getValue().id);
        colItemName.setCellValueFactory(c -> c.getValue().itemName);
        colStart.setCellValueFactory(c -> c.getValue().start);
        colCurrent.setCellValueFactory(c -> c.getValue().current);
        colStartTime.setCellValueFactory(c -> c.getValue().startTime);
        colEndTime.setCellValueFactory(c -> c.getValue().endTime);
        colStatus.setCellValueFactory(c -> c.getValue().status);

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }
                Label l = new Label(item);
                String s = item.toUpperCase();
                if (s.contains("OPEN"))       l.getStyleClass().add("status-open");
                else if (s.contains("CLOSE")) l.getStyleClass().add("status-closed");
                else                          l.getStyleClass().add("status-end");
                setGraphic(l); setText(null);
            }
        });

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("👁 Xem");
            {
                btn.getStyleClass().add("btn-ghost");
                btn.setOnAction(e -> {
                    AuctionRow r = getTableView().getItems().get(getIndex());
                    try { Session.get().setSelectedAuctionId(Long.parseLong(r.id.get())); } catch (Exception ignored) {}
                    MainWindowController main = MainWindowController.get();
                    if (main != null) main.showAuctionDetail();
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) setGraphic(null); else setGraphic(btn);
            }
        });

        tblAuctions.setItems(data);
        handleReload();
    }

    @FXML
    public void handleReload() {
        data.clear();
        if (!Session.get().isLoggedIn()) return;
        JsonObject payload = new JsonObject();
        payload.addProperty("sellerId", Session.get().getUserId());
        String json = NetworkClient.getInstance().sendAndReceive("LIST_MY_AUCTIONS", payload);
        if (json == null) return;

        try {
            JsonElement el = JsonParser.parseString(json);
            JsonArray arr = null;
            if (el.isJsonArray()) arr = el.getAsJsonArray();
            else if (el.isJsonObject() && el.getAsJsonObject().has("auctions"))
                arr = el.getAsJsonObject().getAsJsonArray("auctions");
            if (arr == null) return;
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                AuctionRow r = new AuctionRow();
                r.id.set(getStr(o, "id"));
                r.itemName.set(getStr(o, "itemName"));
                r.start.set(fmt.format(getDouble(o, "startingPrice")) + " ₫");
                r.current.set(fmt.format(getDouble(o, "currentPrice")) + " ₫");
                r.startTime.set(getStr(o, "startTime"));
                r.endTime.set(getStr(o, "endTime"));
                r.status.set(getStr(o, "status"));
                data.add(r);
            }
        } catch (Exception ignored) {}
    }

    @FXML
    public void handleNewAuction() {
        Session.get().setSelectedItemId(null);
        MainWindowController main = MainWindowController.get();
        if (main != null) main.showOpenAuction();
    }

    private static String getStr(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }
    private static double getDouble(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0;
    }

    public static class AuctionRow {
        final SimpleStringProperty id        = new SimpleStringProperty();
        final SimpleStringProperty itemName  = new SimpleStringProperty();
        final SimpleStringProperty start     = new SimpleStringProperty();
        final SimpleStringProperty current   = new SimpleStringProperty();
        final SimpleStringProperty startTime = new SimpleStringProperty();
        final SimpleStringProperty endTime   = new SimpleStringProperty();
        final SimpleStringProperty status    = new SimpleStringProperty();
    }
}
