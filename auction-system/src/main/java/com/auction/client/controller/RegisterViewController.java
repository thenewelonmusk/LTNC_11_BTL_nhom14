package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.dto.RegisterRequest;
import com.auction.dto.RegisterResponse;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterViewController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private PasswordField txtConfirmPassword;
    @FXML private ComboBox<String> cbRole;

    private final Gson gson = new Gson();

    @FXML
    public void initialize() {
        cbRole.getSelectionModel().selectFirst();
    }

    @FXML
    public void handleRegister() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String confirm  = txtConfirmPassword.getText();
        String role     = cbRole.getValue();

        if (username == null || username.isBlank()
                || password == null || password.isBlank()
                || confirm  == null || confirm.isBlank()
                || role     == null || role.isBlank()) {
            alert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ các trường.");
            return;
        }
        if (!password.equals(confirm)) {
            alert(Alert.AlertType.WARNING, "Mật khẩu không khớp", "Mật khẩu xác nhận không trùng khớp.");
            return;
        }

        RegisterRequest req = new RegisterRequest(username, password, confirm, role);
        String responseJson = NetworkClient.getInstance().sendAndReceive("REGISTER", req);

        if (responseJson == null) {
            alert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến máy chủ.");
            return;
        }
        RegisterResponse res = gson.fromJson(responseJson, RegisterResponse.class);
        if (res != null && res.isSuccess()) {
            alert(Alert.AlertType.INFORMATION, "Thành công", res.getMessage());
            goLogin();
        } else {
            String msg = (res != null) ? res.getMessage() : "Phản hồi không hợp lệ.";
            alert(Alert.AlertType.ERROR, "Lỗi đăng ký", msg);
        }
    }

    @FXML
    public void goLogin() {
        MainWindowController main = MainWindowController.get();
        if (main != null) main.showLogin();
    }

    private void alert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
