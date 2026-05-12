package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.dto.LoginRequest;
import com.auction.dto.LoginResponse;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginUIController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    private Gson gson = new Gson();

    @FXML
    public void handleLoginButton() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();

        // 1. Tạo DTO
        LoginRequest req = new LoginRequest(username, password);

        // 2. Gửi qua Socket thông qua NetworkClient
        NetworkClient client = NetworkClient.getInstance();
        String responseJson = client.sendAndReceive("LOGIN", req);

        // 3. Xử lý kết quả trả về
        if (responseJson != null) {
            LoginResponse response = gson.fromJson(responseJson, LoginResponse.class);

            if (response.isSuccess()) {
                showAlert(Alert.AlertType.INFORMATION, "Thành công", response.getMessage());
                // TODO: Chuyển sang màn hình danh sách sản phẩm (Dashboard)
            } else {
                showAlert(Alert.AlertType.ERROR, "Lỗi đăng nhập", response.getMessage());
            }
        } else {
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến máy chủ.");
        }
    }

    @FXML
    public void goToRegister() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Register.fxml"));
            // Lấy Stage hiện tại từ một node bất kỳ (txtUsername)
            Stage stage = (Stage) txtUsername.getScene().getWindow();
            // Form đăng ký dài hơn nên set chiều cao là 450
            stage.setScene(new Scene(root, 400, 450));
            stage.setTitle("Đăng ký - Hệ Thống Đấu Giá");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}