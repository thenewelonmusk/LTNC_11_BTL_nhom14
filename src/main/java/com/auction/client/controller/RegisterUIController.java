package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.dto.RegisterRequest;
import com.auction.dto.RegisterResponse;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterUIController {

	@FXML
	private TextField txtUsername;
	@FXML
	private PasswordField txtPassword;
	@FXML
	private PasswordField txtConfirmPassword;
	@FXML
	private ComboBox<String> cbRole;

	private Gson gson = new Gson();

	@FXML
	public void initialize() {
		// Mặc định chọn BIDDER cho người dùng đỡ phải click nhiều
		cbRole.getSelectionModel().selectFirst();
	}

	@FXML
	public void handleRegisterButton() {
		String username = txtUsername.getText();
		String password = txtPassword.getText();
		String confirmPassword = txtConfirmPassword.getText();
		String role = cbRole.getValue();

		RegisterRequest req = new RegisterRequest(username, password, confirmPassword, role);

		NetworkClient client = NetworkClient.getInstance();
		String responseJson = client.sendAndReceive("REGISTER", req);

		if (responseJson != null) {
			RegisterResponse response = gson.fromJson(responseJson, RegisterResponse.class);
			if (response.isSuccess()) {
				showAlert(Alert.AlertType.INFORMATION, "Thành công", response.getMessage());
				goToLogin(); // Đăng ký xong thì tự chuyển về màn Login
			} else {
				showAlert(Alert.AlertType.ERROR, "Lỗi đăng ký", response.getMessage());
			}
		} else {
			showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", "Không thể kết nối đến máy chủ.");
		}
	}

	@FXML
	public void goToLogin() {
		try {
			Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
			Stage stage = (Stage) txtUsername.getScene().getWindow();
			stage.setScene(new Scene(root, 400, 300));
			stage.setTitle("Đăng nhập - Hệ Thống Đấu Giá");
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