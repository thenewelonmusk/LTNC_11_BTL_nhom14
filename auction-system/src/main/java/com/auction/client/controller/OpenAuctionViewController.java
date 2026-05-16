package com.auction.client.controller;

import com.auction.client.Session;
import com.auction.client.network.NetworkClient;
import com.auction.dto.AuctionRequest;
import com.auction.dto.AuctionResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class OpenAuctionViewController {

    @FXML private ComboBox<ItemOption> cbItem;
    @FXML private TextField txtStartPrice;
    @FXML private DatePicker dpStart, dpEnd;
    @FXML private TextField txtStartTime, txtEndTime;

    private final ObservableList<ItemOption> itemOptions = FXCollections.observableArrayList();

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                @Override public void write(JsonWriter out, LocalDateTime value) throws IOException {
                    out.value(value != null ? value.toString() : null);
                }
                @Override public LocalDateTime read(JsonReader in) throws IOException {
                    return LocalDateTime.parse(in.nextString());
                }
            }).create();

    @FXML
    public void initialize() {
        cbItem.setItems(itemOptions);
        cbItem.setConverter(new StringConverter<>() {
            @Override public String toString(ItemOption o) { return o == null ? "" : ("#" + o.id + " · " + o.name); }
            @Override public ItemOption fromString(String s) { return null; }
        });

        // Default thời gian
        dpStart.setValue(LocalDate.now());
        dpEnd.setValue(LocalDate.now().plusDays(1));
        txtStartTime.setText(LocalTime.now().withSecond(0).withNano(0)
                .format(DateTimeFormatter.ofPattern("HH:mm")));
        txtEndTime.setText("18:00");

        handleReloadItems();

        // Nếu có item được chọn sẵn từ MyItems thì set giá khởi điểm
        Long sel = Session.get().getSelectedItemId();
        if (sel != null) {
            for (ItemOption o : itemOptions) {
                if (sel.equals(o.id)) {
                    cbItem.setValue(o);
                    txtStartPrice.setText(String.valueOf((long) o.startingPrice));
                    break;
                }
            }
        }
    }

    @FXML
    public void handleReloadItems() {
        itemOptions.clear();
        if (!Session.get().isLoggedIn()) return;
        JsonObject payload = new JsonObject();
        payload.addProperty("sellerId", Session.get().getUserId());
        String json = NetworkClient.getInstance().sendAndReceive("LIST_MY_ITEMS", payload);
        if (json == null) return;
        try {
            JsonElement el = JsonParser.parseString(json);
            JsonArray arr = null;
            if (el.isJsonArray()) arr = el.getAsJsonArray();
            else if (el.isJsonObject() && el.getAsJsonObject().has("items"))
                arr = el.getAsJsonObject().getAsJsonArray("items");
            if (arr == null) return;
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                ItemOption opt = new ItemOption();
                opt.id   = o.has("id") && !o.get("id").isJsonNull() ? o.get("id").getAsLong() : null;
                opt.name = o.has("name") && !o.get("name").isJsonNull() ? o.get("name").getAsString() : "(no name)";
                opt.startingPrice = o.has("startingPrice") && !o.get("startingPrice").isJsonNull()
                        ? o.get("startingPrice").getAsDouble() : 0;
                itemOptions.add(opt);
            }
        } catch (Exception ignored) {}
    }

    @FXML
    public void handleBack() {
        MainWindowController main = MainWindowController.get();
        if (main != null) main.showMyAuctions();
    }

    @FXML
    public void handleOpenAuction() {
        if (!Session.get().isLoggedIn() || !Session.get().isSeller()) {
            alert(Alert.AlertType.WARNING, "Không có quyền", "Bạn cần đăng nhập với vai trò SELLER.");
            return;
        }

        ItemOption sel = cbItem.getValue();
        if (sel == null || sel.id == null) {
            alert(Alert.AlertType.WARNING, "Chưa chọn sản phẩm", "Vui lòng chọn sản phẩm cần đấu giá.");
            return;
        }
        double price;
        try { price = Double.parseDouble(txtStartPrice.getText().trim()); }
        catch (Exception e) {
            alert(Alert.AlertType.WARNING, "Giá không hợp lệ", "Giá khởi điểm phải là số.");
            return;
        }
        LocalDateTime start, end;
        try {
            start = LocalDateTime.of(dpStart.getValue(), LocalTime.parse(txtStartTime.getText().trim()));
            end   = LocalDateTime.of(dpEnd.getValue(),   LocalTime.parse(txtEndTime.getText().trim()));
        } catch (Exception e) {
            alert(Alert.AlertType.WARNING, "Thời gian không hợp lệ", "Vui lòng kiểm tra lại ngày/giờ.");
            return;
        }
        if (!end.isAfter(start)) {
            alert(Alert.AlertType.WARNING, "Thời gian không hợp lệ", "Thời điểm kết thúc phải sau thời điểm bắt đầu.");
            return;
        }

        AuctionRequest req = new AuctionRequest();
        req.setItemId(sel.id);
        req.setSellerId(Session.get().getUserId());
        req.setStartingPrice(price);
        req.setStartTime(start);
        req.setEndTime(end);

        String json = NetworkClient.getInstance().sendAndReceive("OPEN_AUCTION", req);
        if (json == null) {
            alert(Alert.AlertType.ERROR, "Lỗi", "Không nhận được phản hồi từ Server.");
            return;
        }
        AuctionResponse res = gson.fromJson(json, AuctionResponse.class);
        if (res == null) {
            alert(Alert.AlertType.ERROR, "Lỗi", "Phản hồi từ Server không hợp lệ.");
            return;
        }
        alert(res.isSuccess() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
              res.isSuccess() ? "Thành công" : "Thất bại",
              res.getMessage());
        if (res.isSuccess()) {
            MainWindowController main = MainWindowController.get();
            if (main != null) main.showMyAuctions();
        }
    }

    private void alert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }

    public static class ItemOption {
        Long id;
        String name;
        double startingPrice;
    }
}
