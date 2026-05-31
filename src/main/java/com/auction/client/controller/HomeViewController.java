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
				javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("");
				dialog.setTitle("Vào phòng đấu giá");
				dialog.setHeaderText("Nhập ID phiên đấu giá bạn muốn truy cập:");
				dialog.setContentText("Mã phiên (ID):");

				dialog.showAndWait().ifPresent(idStr -> {
					try {
						Long auctionId = Long.parseLong(idStr.trim());

						main.showAuctionDetail(auctionId);
					} catch (NumberFormatException e) {
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
