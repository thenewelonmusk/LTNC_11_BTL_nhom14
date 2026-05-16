package com.auction.client.controller;

import com.auction.client.Session;
import com.auction.client.network.NetworkClient;
import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginViewController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    private final Gson gson = new Gson();

    @FXML
    public void handleLogin() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            alert(Alert.AlertType.WARNING, "Thiếu thông tin", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return;
        }

        LoginRequest req = new LoginRequest(username, password);
        String responseJson = NetworkClient.getInstance().sendAndReceive("LOGIN", req);

        if (responseJson == null) {
            alert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến máy chủ.");
            return;
        }

        LoginResponse res = gson.fromJson(responseJson, LoginResponse.class);
        if (res != null && res.isSuccess()) {
            Session.get().loginAs(res.getId(), res.getUsername(), res.getRole());
            MainWindowController main = MainWindowController.get();
            if (main != null) main.onLoginSuccess();
        } else {
            String msg = (res != null) ? res.getMessage() : "Phản hồi không hợp lệ.";
            alert(Alert.AlertType.ERROR, "Lỗi đăng nhập", msg);
        }
    }

    @FXML
    public void goRegister() {
        MainWindowController main = MainWindowController.get();
        if (main != null) main.showRegister();
    }

    private void alert(Alert.AlertType type, String title, String content) {
        Alert a = new Alert(type);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }
}
