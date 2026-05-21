package com.auction.client.controller;

import com.auction.client.Session;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller chính cho cửa sổ duy nhất. Quản lý header, sidebar (theo role) và
 * content area. Các view con sẽ được load vào contentArea bằng phương thức
 * showView*.
 */
public class MainWindowController {

	@FXML
	private BorderPane rootPane;
	@FXML
	private HBox header;
	@FXML
	private VBox sidebar;
	@FXML
	private StackPane contentArea;

	@FXML
	private Label lblUserChip;
	@FXML
	private Button btnLogout;

	// Sidebar buttons
	@FXML
	private Button navHome;
	@FXML
	private Label lblSecBidder, lblSecSeller;
	@FXML
	private Button navBrowse, navAuctionDetail, navMyBids;
	@FXML
	private Button navMyItems, navItemForm, navMyAuctions, navOpenAuction;

	private final List<Button> allNavButtons = new ArrayList<>();

	/** Singleton để các view con có thể yêu cầu MainWindow chuyển trang. */
	private static MainWindowController instance;
	public static MainWindowController get() {
		return instance;
	}

	@FXML
	public void initialize() {
		instance = this;

		allNavButtons.add(navHome);
		allNavButtons.add(navBrowse);
		allNavButtons.add(navAuctionDetail);
		allNavButtons.add(navMyBids);
		allNavButtons.add(navMyItems);
		allNavButtons.add(navItemForm);
		allNavButtons.add(navMyAuctions);
		allNavButtons.add(navOpenAuction);

		// Khởi đầu: hiển thị màn Login
		showLogin();
	}

	// ===== Navigation helpers =====

	/** Tải FXML vào contentArea. */
	private void loadView(String fxmlPath) {
		try {
			URL url = getClass().getResource(fxmlPath);
			if (url == null) {
				showError("Không tìm thấy view: " + fxmlPath);
				return;
			}
			Node view = FXMLLoader.load(url);
			contentArea.getChildren().setAll(view);
		} catch (IOException e) {
			e.printStackTrace();
			showError("Lỗi tải view: " + e.getMessage());
		}
	}

	private void showError(String msg) {
		Label l = new Label(msg);
		l.setStyle("-fx-text-fill: #c0392b; -fx-font-size: 14px;");
		contentArea.getChildren().setAll(l);
	}

	/** Gọi khi đăng nhập thành công – cập nhật UI theo role. */
	public void onLoginSuccess() {
		applyAuthState(true);
		showHome();
	}

	/** Cập nhật UI tùy theo trạng thái đăng nhập. */
	private void applyAuthState(boolean loggedIn) {
		// Header
		boolean show = loggedIn;
		btnLogout.setVisible(show);
		btnLogout.setManaged(show);

		if (loggedIn) {
			Session s = Session.get();
			lblUserChip.setText("👤 " + s.getUsername() + "  ·  " + s.getRole());
		} else {
			lblUserChip.setText("Chưa đăng nhập");
		}

		// Sidebar visibility
		sidebar.setVisible(loggedIn);
		sidebar.setManaged(loggedIn);

		if (loggedIn) {
			boolean bidder = Session.get().isBidder();
			boolean seller = Session.get().isSeller();

			// Section labels
			lblSecBidder.setVisible(bidder);
			lblSecBidder.setManaged(bidder);
			lblSecSeller.setVisible(seller);
			lblSecSeller.setManaged(seller);

			// Bidder buttons
			for (Button b : new Button[]{navBrowse, navAuctionDetail, navMyBids}) {
				b.setVisible(bidder);
				b.setManaged(bidder);
			}
			// Seller buttons
			for (Button b : new Button[]{navMyItems, navItemForm, navMyAuctions, navOpenAuction}) {
				b.setVisible(seller);
				b.setManaged(seller);
			}
		}
	}

	/** Đánh dấu nút đang active. */
	private void setActive(Button activeBtn) {
		for (Button b : allNavButtons)
			b.getStyleClass().remove("active");
		if (activeBtn != null && !activeBtn.getStyleClass().contains("active")) {
			activeBtn.getStyleClass().add("active");
		}
	}

	// ===== Public navigation API =====

	public void showLogin() {
		applyAuthState(false);
		setActive(null);
		loadView("/fxml/views/LoginView.fxml");
	}

	public void showRegister() {
		applyAuthState(false);
		setActive(null);
		loadView("/fxml/views/RegisterView.fxml");
	}

	@FXML
	public void showHome() {
		setActive(navHome);
		loadView("/fxml/views/HomeView.fxml");
	}
	@FXML
	public void showBrowseAuctions() {
		setActive(navBrowse);
		loadView("/fxml/views/BrowseAuctionsView.fxml");
	}
	@FXML
	public void showAuctionDetail() {
		setActive(navAuctionDetail);
		loadView("/fxml/views/AuctionDetailView.fxml");
	}
	@FXML
	public void showMyBids() {
		setActive(navMyBids);
		loadView("/fxml/views/MyBidsView.fxml");
	}
	@FXML
	public void showMyItems() {
		setActive(navMyItems);
		loadView("/fxml/views/MyItemsView.fxml");
	}
	@FXML
	public void showItemForm() {
		setActive(navItemForm);
		loadView("/fxml/views/ItemFormView.fxml");
	}
	@FXML
	public void showMyAuctions() {
		setActive(navMyAuctions);
		loadView("/fxml/views/MyAuctionsView.fxml");
	}
	@FXML
	public void showOpenAuction() {
		setActive(navOpenAuction);
		loadView("/fxml/views/OpenAuctionView.fxml");
	}

	@FXML
	public void handleLogout() {
		Session.get().logout();
		showLogin();
	}
}
