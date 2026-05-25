package com.auction.client.controller;

import com.auction.client.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

public class HomeViewController {

	@FXML
	private Label lblWelcome;
	@FXML
	private Label lblRole;
	@FXML
	private Label lblUserId;
	@FXML
	private Label lblConn;
	@FXML
	private FlowPane quickActions;

	@FXML
	public void initialize() {
		Session s = Session.get();
		if (!s.isLoggedIn()) {
			lblWelcome.setText("Chưa đăng nhập");
			lblRole.setText("-");
			lblUserId.setText("-");
		} else {
			lblWelcome.setText("Chào mừng quay lại, " + s.getUsername() + " 👋");
			lblRole.setText(s.getRole());
			lblUserId.setText(String.valueOf(s.getUserId()));
		}
		lblConn.setText("Đã sẵn sàng");

		buildQuickActions();
	}

	private void buildQuickActions() {
		quickActions.getChildren().clear();
		MainWindowController main = MainWindowController.get();
		if (main == null)
			return;

		if (Session.get().isBidder()) {
			quickActions.getChildren().add(action("🔍 Xem phiên đấu giá", "btn-primary", main::showBrowseAuctions));
			quickActions.getChildren().add(action("🎯 Vào chi tiết phiên", "btn-warning", () -> {
				// 1. Tạo một hộp thoại yêu cầu nhập ID phòng muốn vào
				javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("");
				dialog.setTitle("Vào phòng đấu giá");
				dialog.setHeaderText("Nhập ID phiên đấu giá bạn muốn truy cập:");
				dialog.setContentText("Mã phiên (ID):");

				// 2. Chờ người dùng nhập số và nhấn OK
				dialog.showAndWait().ifPresent(idStr -> {
					try {
						// Chuyển chuỗi nhập vào thành kiểu Long
						Long auctionId = Long.parseLong(idStr.trim());

						// Gọi hàm chuyển giao diện chính xác với ID người dùng vừa nhập
						main.showAuctionDetail(auctionId);
					} catch (NumberFormatException e) {
						// Xử lý an toàn nếu người dùng cố tình nhập chữ thay vì nhập số
						javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
								javafx.scene.control.Alert.AlertType.ERROR);
						alert.setTitle("Lỗi định dạng");
						alert.setHeaderText(null);
						alert.setContentText("Mã phiên đấu giá bắt buộc phải là một số nguyên hợp lệ!");
						alert.showAndWait();
					}
				});
			}));
			quickActions.getChildren().add(action("📜 Lịch sử bid của tôi", "btn-ghost", main::showMyBids));
		} else if (Session.get().isSeller()) {
			quickActions.getChildren().add(action("➕ Tạo sản phẩm mới", "btn-success", main::showItemForm));
			quickActions.getChildren().add(action("📦 Quản lý sản phẩm", "btn-primary", main::showMyItems));
			quickActions.getChildren().add(action("🚀 Mở phiên đấu giá", "btn-warning", main::showOpenAuction));
			quickActions.getChildren().add(action("🛎 Phiên của tôi", "btn-ghost", main::showMyAuctions));
		}
	}

	private Button action(String text, String style, Runnable r) {
		Button b = new Button(text);
		b.getStyleClass().add(style);
		b.setOnAction(e -> r.run());
		return b;
	}
}
