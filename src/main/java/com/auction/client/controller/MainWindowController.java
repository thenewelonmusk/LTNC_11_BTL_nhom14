package com.auction.client.controller;

import com.auction.client.Session;
import com.auction.client.network.NetworkClient;
import com.google.gson.JsonObject;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainWindowController implements NetworkClient.AuctionUpdateListener {

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

	@FXML
	private Button navHome;
	@FXML
	private Label lblSecBidder, lblSecSeller;
	@FXML
	private Button navBrowse, navMyBids;
	@FXML
	private Button navMyItems, navItemForm, navMyAuctions, navOpenAuction;

	private final List<Button> allNavButtons = new ArrayList<>();

	private String currentViewPath;
	private Object currentViewController;

	private static MainWindowController instance;

	public static MainWindowController get() {
		return instance;
	}

	@FXML
	public void initialize() {
		instance = this;

		allNavButtons.add(navHome);
		allNavButtons.add(navBrowse);
		allNavButtons.add(navMyBids);
		allNavButtons.add(navMyItems);
		allNavButtons.add(navItemForm);
		allNavButtons.add(navMyAuctions);
		allNavButtons.add(navOpenAuction);

		NetworkClient.getInstance().addListener(this);

		showLogin();
	}

	public void loadView(String fxmlPath) {
		try {
			URL url = getClass().getResource(fxmlPath);
			if (url == null) {
				showError("Không tìm thấy view: " + fxmlPath);
				return;
			}

			cleanupCurrentView();

			FXMLLoader loader = new FXMLLoader(url);
			Node view = loader.load();
			contentArea.getChildren().setAll(view);

			currentViewPath = fxmlPath;
			currentViewController = loader.getController();

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

	private void cleanupCurrentView() {
		if (currentViewController != null) {
			invokeIfExists(currentViewController, "destroy");
		}
		currentViewController = null;
		currentViewPath = null;
	}

	private void invokeIfExists(Object target, String methodName) {
		try {
			Method m = target.getClass().getMethod(methodName);
			m.invoke(target);
		} catch (NoSuchMethodException ignored) {
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void onLoginSuccess() {
		applyAuthState(true);
		showHome();
	}

	private void applyAuthState(boolean loggedIn) {
		btnLogout.setVisible(loggedIn);
		btnLogout.setManaged(loggedIn);

		if (loggedIn) {
			Session s = Session.get();
			lblUserChip.setText("👤 " + s.getUsername() + "  ·  " + s.getRole());
		} else {
			lblUserChip.setText("Chưa đăng nhập");
		}

		sidebar.setVisible(loggedIn);
		sidebar.setManaged(loggedIn);

		if (loggedIn) {
			boolean bidder = Session.get().isBidder();
			boolean seller = Session.get().isSeller();

			lblSecBidder.setVisible(bidder);
			lblSecBidder.setManaged(bidder);
			lblSecSeller.setVisible(seller);
			lblSecSeller.setManaged(seller);

			// Bidder buttons
			for (Button b : new Button[]{navBrowse, navMyBids}) {
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

	private void setActive(Button activeBtn) {
		for (Button b : allNavButtons) {
			b.getStyleClass().remove("active");
		}
		if (activeBtn != null && !activeBtn.getStyleClass().contains("active")) {
			activeBtn.getStyleClass().add("active");
		}
	}

	public void showLogin() {
		cleanupCurrentView();
		applyAuthState(false);
		setActive(null);
		loadView("/fxml/views/LoginView.fxml");
	}

	public void showRegister() {
		cleanupCurrentView();
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

	public void showAuctionDetail() {
		showBrowseAuctions();
	}

	public void showAuctionDetail(Long auctionId) {
		if (auctionId == null) {
			System.err.println("[LỖI] ID phiên đấu giá truyền vào bị null.");
			return;
		}

		try {
			cleanupCurrentView();

			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/views/AuctionDetailView.fxml"));
			Parent view = loader.load();

			Object controllerObj = loader.getController();
			if (controllerObj instanceof AuctionDetailViewController controller) {
				controller.setMainWindowController(this);
				controller.loadAuctionDetail(auctionId);
			} else {
				System.err.println("[LỖI] Không thể ép kiểu sang AuctionDetailViewController.");
				return;
			}

			contentArea.getChildren().setAll(view);
			currentViewPath = "/fxml/views/AuctionDetailView.fxml";
			currentViewController = controllerObj;

			setActive(navBrowse);

		} catch (IOException e) {
			System.err.println("[LỖI] Không thể tải /fxml/views/AuctionDetailView.fxml.");
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("[LỖI HỆ THỐNG] Phát sinh lỗi khi chuyển cảnh: " + e.getMessage());
			e.printStackTrace();
		}
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
		cleanupCurrentView();
		Session.get().logout();
		showLogin();
	}

	@Override
	public void onAuctionUpdate(JsonObject data) {
		if (data == null) {
			return;
		}

		Platform.runLater(() -> {
			if (currentViewController instanceof BrowseAuctionsViewController browse) {
				browse.handleSearch();
				return;
			}

			if (currentViewController instanceof MyAuctionsViewController myAuctions) {
				myAuctions.handleReload();
				return;
			}

			if (currentViewController instanceof MyBidsViewController myBids) {
				myBids.handleReload();
			}
		});
	}
}
