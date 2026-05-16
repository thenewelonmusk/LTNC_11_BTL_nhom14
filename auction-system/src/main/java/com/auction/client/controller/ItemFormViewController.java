package com.auction.client.controller;

import com.auction.client.Session;
import com.auction.client.network.NetworkClient;
import com.auction.dto.ItemRequest;
import com.auction.dto.ItemResponse;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class ItemFormViewController {

    @FXML private Label lblTitle;
    @FXML private TextField txtName, txtPrice;
    @FXML private TextArea  txtDesc;
    @FXML private ComboBox<String> cbCategory;

    // Extra type-specific blocks
    @FXML private VBox boxElectronics, boxArt, boxVehicle;
    @FXML private TextField txtDeviceBrand, txtWarrantyMonths;
    @FXML private TextField txtArtist, txtYear;
    @FXML private TextField txtVehicleBrand, txtMileage;

    private Long currentId = null;
    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        currentId = Session.get().getSelectedItemId();
        if (currentId != null) {
            lblTitle.setText("✏ Chỉnh sửa sản phẩm #" + currentId);
        } else {
            lblTitle.setText("➕ Tạo sản phẩm mới");
        }

        cbCategory.valueProperty().addListener((obs, oldV, newV) -> updateTypeFields(newV));
        updateTypeFields(cbCategory.getValue());
    }

    private void updateTypeFields(String type) {
        boolean isElec = "ELECTRONICS".equals(type);
        boolean isArt  = "ART".equals(type);
        boolean isVeh  = "VEHICLE".equals(type);
        boxElectronics.setVisible(isElec); boxElectronics.setManaged(isElec);
        boxArt.setVisible(isArt);          boxArt.setManaged(isArt);
        boxVehicle.setVisible(isVeh);      boxVehicle.setManaged(isVeh);
    }

    @FXML
    public void handleBack() {
        MainWindowController main = MainWindowController.get();
        if (main != null) main.showMyItems();
    }

    @FXML
    public void handleSave() {
        if (!Session.get().isLoggedIn() || !Session.get().isSeller()) {
            alert(Alert.AlertType.WARNING, "Không có quyền", "Bạn phải đăng nhập với vai trò SELLER.");
            return;
        }
        if (isBlank(txtName) || cbCategory.getValue() == null) {
            alert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập tên sản phẩm và chọn danh mục.");
            return;
        }
        double price;
        try { price = Double.parseDouble(txtPrice.getText().trim()); }
        catch (Exception e) {
            alert(Alert.AlertType.WARNING, "Giá không hợp lệ", "Giá khởi điểm phải là một số.");
            return;
        }

        ItemRequest req = new ItemRequest();
        req.setItemId(currentId);
        req.setSellerId(Session.get().getUserId());
        req.setName(txtName.getText().trim());
        req.setDescription(txtDesc.getText());
        req.setType(cbCategory.getValue());
        req.setStartingPrice(price);

        try {
            switch (cbCategory.getValue()) {
                case "ELECTRONICS":
                    req.setDeviceBrand(txtDeviceBrand.getText());
                    if (!isBlank(txtWarrantyMonths))
                        req.setWarrantyMonths(Integer.parseInt(txtWarrantyMonths.getText().trim()));
                    break;
                case "ART":
                    req.setArtist(txtArtist.getText());
                    if (!isBlank(txtYear))
                        req.setYear(Integer.parseInt(txtYear.getText().trim()));
                    break;
                case "VEHICLE":
                    req.setVehicleBrand(txtVehicleBrand.getText());
                    if (!isBlank(txtMileage))
                        req.setMileage(Integer.parseInt(txtMileage.getText().trim()));
                    break;
            }
        } catch (NumberFormatException ex) {
            alert(Alert.AlertType.WARNING, "Số không hợp lệ", "Vui lòng nhập số hợp lệ cho các trường số.");
            return;
        }

        String json = NetworkClient.getInstance().sendAndReceive("SAVE_ITEM", req);
        if (json == null || json.equals("null")) {
            alert(Alert.AlertType.ERROR, "Lỗi", "Không nhận được phản hồi từ Server.");
            return;
        }
        ItemResponse res = gson.fromJson(json, ItemResponse.class);
        if (res == null) {
            alert(Alert.AlertType.ERROR, "Lỗi", "Phản hồi từ Server không hợp lệ.");
            return;
        }
        alert(res.isSuccess() ? Alert.AlertType.INFORMATION : Alert.AlertType.WARNING,
              res.isSuccess() ? "Thành công" : "Thất bại", res.getMessage());
        if (res.isSuccess() && res.getItemData() != null) {
            currentId = res.getItemData().getItemId();
            Session.get().setSelectedItemId(currentId);
            lblTitle.setText("✏ Chỉnh sửa sản phẩm #" + currentId);
        }
    }

    private boolean isBlank(TextField f) { return f.getText() == null || f.getText().isBlank(); }

    private void alert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }
}
