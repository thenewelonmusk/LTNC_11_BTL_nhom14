package com.auction.client.controller;

import com.auction.client.Session;
import com.auction.client.network.NetworkClient;
import com.auction.dto.BidRequest;
import com.auction.dto.BidResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;

public class AuctionDetailViewController {

    @FXML
    private TextField txtAuctionId;
    @FXML
    private Label lblItemName, lblItemType, lblItemDesc;
    @FXML
    private Label lblStatus, lblStart, lblCurrent, lblEnd;
    @FXML
    private TextField txtBidAmount;
    @FXML
    private Label lblBidResult;

    @FXML
    private TableView<BidRow> tblBids;
    @FXML
    private TableColumn<BidRow, String> colBidId;
    @FXML
    private TableColumn<BidRow, String> colBidder;
    @FXML
    private TableColumn<BidRow, String> colAmount;
    @FXML
    private TableColumn<BidRow, String> colBidTime;

    private final ObservableList<BidRow> bids = FXCollections.observableArrayList();
    private final NumberFormat fmt = NumberFormat.getInstance(new Locale("vi", "VN"));
    private double currentPrice = 0;

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
	
    @FXML
    public void initialize() {
        colBidId.setCellValueFactory(c -> c.getValue().id);
        colBidder.setCellValueFactory(c -> c.getValue().bidder);
        colAmount.setCellValueFactory(c -> c.getValue().amount);
        colBidTime.setCellValueFactory(c -> c.getValue().time);
        tblBids.setItems(bids);

        // Nếu đã có ID chọn sẵn từ màn danh sách
        Long sel = Session.get().getSelectedAuctionId();
        if (sel != null) {
            txtAuctionId.setText(String.valueOf(sel));
            handleLoad();
        }
    }

    @FXML
    public void handleBack() {
        MainWindowController main = MainWindowController.get();
        if (main != null)
            main.showBrowseAuctions();
    }

    /**
     * Gọi server "GET_AUCTION_DETAIL" để lấy chi tiết. Nếu server chưa hỗ trợ, chỉ
     * hiển thị ID/Trạng thái với giá trị mặc định.
     */
    @FXML
    public void handleLoad() {
        Long id = parseLong(txtAuctionId.getText());
        if (id == null) {
            alert(Alert.AlertType.WARNING, "ID không hợp lệ", "Vui lòng nhập ID phiên đấu giá hợp lệ.");
            return;
        }
        JsonObject payload = new JsonObject();
        payload.addProperty("auctionId", id);

        String json = NetworkClient.getInstance().sendAndReceive("GET_AUCTION_DETAIL", payload);
        if (json == null)
            return;

        try {
            JsonElement el = JsonParser.parseString(json);
            if (!el.isJsonObject())
                return;
            JsonObject o = el.getAsJsonObject();

            if (o.has("auction") && o.get("auction").isJsonObject()) {
                JsonObject a = o.getAsJsonObject("auction");
                lblStart.setText(fmt.format(getDouble(a, "startingPrice")) + " ₫");
                currentPrice = getDouble(a, "currentPrice");
                lblCurrent.setText(fmt.format(currentPrice) + " ₫");
                lblEnd.setText(getStr(a, "endTime"));

                String status = getStr(a, "status");
                lblStatus.setText(status);
                lblStatus.getStyleClass().removeAll("status-open", "status-closed", "status-end");
                if (status.toUpperCase().contains("OPEN"))
                    lblStatus.getStyleClass().add("status-open");
                else if (status.toUpperCase().contains("CLOSE"))
                    lblStatus.getStyleClass().add("status-closed");
                else
                    lblStatus.getStyleClass().add("status-end");
            }
            if (o.has("item") && o.get("item").isJsonObject()) {
                JsonObject it = o.getAsJsonObject("item");
                lblItemName.setText(getStr(it, "name"));
                lblItemType.setText(getStr(it, "type"));
                lblItemDesc.setText(getStr(it, "description"));
            }
            bids.clear();
            if (o.has("bids") && o.get("bids").isJsonArray()) {
                for (JsonElement be : o.getAsJsonArray("bids")) {
                    JsonObject b = be.getAsJsonObject();
                    BidRow r = new BidRow();
                    r.id.set(getStr(b, "id"));
                    r.bidder.set(getStr(b, "bidderName"));
                    r.amount.set(fmt.format(getDouble(b, "amount")) + " ₫");
                    r.time.set(getStr(b, "bidTime"));
                    bids.add(r);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @FXML
    public void handlePlaceBid() {
        Long auctionId = parseLong(txtAuctionId.getText());
        Double amount = parseDouble(txtBidAmount.getText());

        if (auctionId == null) {
            alert(Alert.AlertType.WARNING, "Thiếu ID phiên", "Hãy chọn/nhập ID phiên đấu giá.");
            return;
        }
        if (amount == null || amount <= 0) {
            alert(Alert.AlertType.WARNING, "Số tiền không hợp lệ", "Vui lòng nhập số tiền hợp lệ.");
            return;
        }
        if (!Session.get().isLoggedIn() || !Session.get().isBidder()) {
            alert(Alert.AlertType.WARNING, "Không có quyền", "Bạn cần đăng nhập với vai trò BIDDER.");
            return;
        }

        BidRequest req = new BidRequest();
        req.setAuctionId(auctionId);
        req.setUserId(Session.get().getUserId());
        req.setAmount(amount);

        String json = NetworkClient.getInstance().sendAndReceive("PLACE_BID", req);
        if (json == null) {
            lblBidResult.setText("Lỗi: Không nhận được phản hồi từ Server.");
            return;
        }
        BidResponse res = gson.fromJson(json, BidResponse.class);
        if (res != null) {
            lblBidResult.setText(res.getMessage());
            if (res.isSuccess()) {
                currentPrice = res.getCurrentPrice();
                lblCurrent.setText(fmt.format(currentPrice) + " ₫");
                handleLoad(); // refresh
            }
        }
    }

    private Long parseLong(String s) {
        try {
            return Long.parseLong(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private Double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private static String getStr(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    private static double getDouble(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0;
    }

    private void alert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    public static class BidRow {
        final SimpleStringProperty id = new SimpleStringProperty();
        final SimpleStringProperty bidder = new SimpleStringProperty();
        final SimpleStringProperty amount = new SimpleStringProperty();
        final SimpleStringProperty time = new SimpleStringProperty();
    }
}
