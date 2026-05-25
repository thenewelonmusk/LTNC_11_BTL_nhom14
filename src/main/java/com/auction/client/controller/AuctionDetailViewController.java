package com.auction.client.controller;

import com.auction.client.Session;
import com.auction.client.network.NetworkClient;
import com.auction.dto.BidRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Controller màn hình "Chi tiết phiên đấu giá".
 *
 * Tính năng: - Hiển thị thông tin sản phẩm + tình trạng phiên - Đặt giá thầu
 * (PLACE_BID) + các nút quick-add (+10k / +50k / +100k) - 📈 Biểu đồ đường giá
 * đấu cao nhất theo thời gian thực (Phần 3.2.5 đề bài) • Trục X: số giây đã
 * trôi qua kể từ lúc mở view • Trục Y: giá đấu hiện tại • Mỗi bid hợp lệ (qua
 * broadcast AUCTION_UPDATE) -> tự động thêm điểm mới - ⏰ Đồng hồ đếm ngược thời
 * gian còn lại của phiên - Highlight bid do CHÍNH user hiện tại đặt (bidder ==
 * session.username) - "🟢 LIVE" indicator nhấp nháy khi đang nhận realtime
 * update
 */
public class AuctionDetailViewController implements NetworkClient.AuctionUpdateListener {

	// ---- Header / search ----
	@FXML
	private TextField txtAuctionId;
	@FXML
	private Label lblLiveIndicator;

	// ---- Item / status ----
	@FXML
	private Label lblItemName;
	@FXML
	private Label lblItemType;
	@FXML
	private Label lblItemDesc;
	@FXML
	private Label lblStatus;
	@FXML
	private Label lblStart;
	@FXML
	private Label lblCurrent;
	@FXML
	private Label lblEnd;
	@FXML
	private Label lblCountdown;

	// ---- Bid input ----
	@FXML
	private TextField txtBidAmount;
	@FXML
	private Button btnPlaceBid;
	@FXML
	private Label lblBidResult;

	// ---- Chart ----
	@FXML
	private LineChart<Number, Number> priceChart;
	@FXML
	private NumberAxis xAxis;
	@FXML
	private NumberAxis yAxis;
	@FXML
	private Label lblBidCount;
	@FXML
	private Label lblPeakPrice;

	// ---- Bid table ----
	@FXML
	private TableView<BidRow> tblBids;
	@FXML
	private TableColumn<BidRow, String> colBidId;
	@FXML
	private TableColumn<BidRow, String> colBidder;
	@FXML
	private TableColumn<BidRow, Double> colAmount;
	@FXML
	private TableColumn<BidRow, String> colBidTime;
	@FXML
	private Label lblHighestBidder;

	private Long currentAuctionId;
	private final ObservableList<BidRow> bidsList = FXCollections.observableArrayList();
	private MainWindowController mainWindowController;

	// Chart state
	private XYChart.Series<Number, Number> priceSeries;
	private double peakPrice = 0.0;
	private int bidCount = 0;

	/**
	 * Mốc thời gian để normalize trục X realtime chart.
	 */
	private long chartBaseTime = System.currentTimeMillis();

	// Countdown
	private Timeline countdownTimeline;
	private Timeline liveBlinkTimeline;
	private LocalDateTime endTime;
	private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

	public void setMainWindowController(MainWindowController mainWindowController) {
		this.mainWindowController = mainWindowController;
	}

	@FXML
	public void initialize() {
		// Bid table columns
		if (colBidId != null) {
			colBidId.setCellValueFactory(cellData -> cellData.getValue().idProperty());
		}
		if (colBidder != null) {
			colBidder.setCellValueFactory(cellData -> cellData.getValue().bidderProperty());
		}
		if (colAmount != null) {
			colAmount.setCellValueFactory(cellData -> cellData.getValue().amountProperty().asObject());
			// Format VNĐ cho cột "Số tiền"
			colAmount.setCellFactory(col -> new TableCell<>() {
				@Override
				protected void updateItem(Double value, boolean empty) {
					super.updateItem(value, empty);
					if (empty || value == null) {
						setText(null);
					} else {
						setText(String.format(Locale.US, "%,.0f ₫", value));
					}
				}
			});
		}
		if (colBidTime != null) {
			colBidTime.setCellValueFactory(cellData -> cellData.getValue().timeProperty());
		}

		if (tblBids != null) {
			tblBids.setItems(bidsList);

			// Highlight row của chính user
			tblBids.setRowFactory(tv -> new TableRow<>() {
				@Override
				protected void updateItem(BidRow item, boolean empty) {
					super.updateItem(item, empty);
					getStyleClass().removeAll("my-bid-row", "top-bid-row");
					if (empty || item == null) {
						return;
					}
					String me = Session.get().getUsername();
					if (me != null && me.equalsIgnoreCase(item.bidderProperty().get())) {
						getStyleClass().add("my-bid-row");
					}
					// bid cao nhất (index 0) cũng được tô màu nổi bật
					if (getIndex() == 0) {
						getStyleClass().add("top-bid-row");
					}
				}
			});
		}

		// Chart series
		if (priceChart != null) {
			priceSeries = new XYChart.Series<>();
			chartBaseTime = System.currentTimeMillis();
			priceSeries.setName("Giá đấu");
			priceChart.getData().add(priceSeries);
		}

		// Đèn LIVE nhấp nháy
		startLiveBlink();

		// Đăng ký nhận broadcast realtime
		NetworkClient.getInstance().addListener(this);
	}

	// =====================================================================
	// LOAD AUCTION
	// =====================================================================

	@FXML
	private void handleLoad() {
		String idText = txtAuctionId != null ? txtAuctionId.getText() : null;
		if (idText == null || idText.trim().isEmpty()) {
			showMessage("Vui lòng nhập ID phiên đấu giá.");
			return;
		}

		try {
			loadAuctionDetail(Long.parseLong(idText.trim()));
		} catch (NumberFormatException e) {
			showMessage("ID phiên đấu giá không hợp lệ.");
		}
	}

	public void loadAuctionDetail(Long auctionId) {
		this.currentAuctionId = auctionId;
		if (txtAuctionId != null) {
			txtAuctionId.setText(String.valueOf(auctionId));
		}

		// Reset chart state cho phiên mới
		resetChart();

		new Thread(() -> {
			JsonObject dataPayload = new JsonObject();
			dataPayload.addProperty("auctionId", auctionId);
			String jsonResponse = NetworkClient.getInstance().sendAndReceive("GET_AUCTION_DETAIL", dataPayload);

			if (jsonResponse != null) {
				Platform.runLater(() -> parseAndPopulateUI(jsonResponse));
			} else {
				Platform.runLater(() -> showMessage("Không nhận được phản hồi từ máy chủ."));
			}
		}).start();
	}

	private void parseAndPopulateUI(String jsonStr) {
		try {
			JsonObject root = JsonParser.parseString(jsonStr).getAsJsonObject();
			if (root.has("success") && !root.get("success").getAsBoolean()) {
				showMessage(getStr(root, "message", "Không tải được chi tiết phiên đấu giá."));
				return;
			}

			JsonObject auction = root.has("auction") && root.get("auction").isJsonObject()
					? root.getAsJsonObject("auction")
					: new JsonObject();
			JsonObject item = root.has("item") && root.get("item").isJsonObject()
					? root.getAsJsonObject("item")
					: new JsonObject();
			JsonArray bids = root.has("bids") && root.get("bids").isJsonArray()
					? root.getAsJsonArray("bids")
					: new JsonArray();

			if (lblItemName != null)
				lblItemName.setText(getStr(item, "name", "-"));
			if (lblItemType != null)
				lblItemType.setText(getStr(item, "type", "-"));
			if (lblItemDesc != null)
				lblItemDesc.setText(getStr(item, "description", "-"));
			double startPrice = getDouble(auction, "startingPrice");
			double currentPrice = getDouble(auction, "currentPrice");
			if (lblStart != null)
				lblStart.setText(formatMoney(startPrice));
			if (lblCurrent != null)
				lblCurrent.setText(formatMoney(currentPrice));
			String endStr = getStr(auction, "endTime", "-");
			if (lblEnd != null)
				lblEnd.setText(endStr);

			String statusText = getStr(auction, "status", "-");
			applyStatusStyle(statusText);

			if (lblBidResult != null)
				lblBidResult.setText("");

			// Setup countdown timer
			setupCountdown(endStr);

			// Populate bid table
			bidsList.clear();
			bidCount = 0;
			peakPrice = startPrice;
			int idx = 1;
			for (JsonElement e : bids) {
				JsonObject b = e.getAsJsonObject();
				String bidId = getStr(b, "bidId", getStr(b, "id", String.valueOf(idx++)));
				String bidderName = getStr(b, "bidderName", getStr(b, "bidder", ""));
				double amount = getDouble(b, "amount");
				String bidTime = getStr(b, "bidTime", getStr(b, "time", ""));
				bidsList.add(new BidRow(bidId, bidderName, amount, bidTime));
				bidCount++;
				if (amount > peakPrice) {
					peakPrice = amount;
				}
			}

			// Sắp xếp bid mới nhất ở đầu (theo amount giảm dần)
			bidsList.sort((a, b) -> Double.compare(b.amountProperty().get(), a.amountProperty().get()));

			// Build initial chart points (historical)
			buildInitialChart(startPrice, currentPrice, bids);

			updateBidStats();
			updateHighestBidderLabel();

		} catch (Exception e) {
			e.printStackTrace();
			showMessage("Lỗi hiển thị dữ liệu phiên.");
		}
	}

	/**
	 * Vẽ chart từ dữ liệu lịch sử khi mới load phòng.
	 */
	private void buildInitialChart(double startingPrice, double currentPrice, JsonArray bids) {
		if (priceSeries == null)
			return;
		priceSeries.getData().clear();
		// Dùng timestamp THẬT của bid đầu tiên thay vì thời điểm mở view

		// Điểm đầu tiên: giá khởi điểm tại t=0
		priceSeries.getData().add(new XYChart.Data<>(0, startingPrice));

		// Mỗi bid lịch sử -> 1 điểm. Vì ta không có timestamp chính xác để
		// vẽ theo thời gian thật, ta phân bố đều theo thứ tự (giả lập "tick").
		int n = bids.size();
		if (n > 0) {
			// Duyệt theo thứ tự thời gian: tăng dần theo amount nếu time không có
			java.util.List<Double> amounts = new java.util.ArrayList<>();
			for (JsonElement el : bids) {
				JsonObject b = el.getAsJsonObject();
				amounts.add(getDouble(b, "amount"));
			}
			int bidIndex = 1;
			for (JsonElement el : bids) {
				JsonObject bid = el.getAsJsonObject();
				double amount = getDouble(bid, "amount");
				priceSeries.getData().add(new XYChart.Data<>(bidIndex++, amount));
			}
		} else if (currentPrice > startingPrice) {
			// Trường hợp không có chi tiết bid nhưng currentPrice đã tăng
			priceSeries.getData().add(new XYChart.Data<>(1, currentPrice));
		}
	}

	private void resetChart() {

		chartBaseTime = System.currentTimeMillis();

		bidCount = 0;
		peakPrice = 0;

		if (priceSeries != null) {
			priceSeries.getData().clear();
		}

		if (lblBidCount != null) {
			lblBidCount.setText("0");
		}

		if (lblPeakPrice != null) {
			lblPeakPrice.setText("0 ₫");
		}
	}

	/**
	 * Thêm điểm realtime vào line chart.
	 *
	 * SỬA: dùng cùng đơn vị với buildInitialChart (chỉ số bid - 1, 2, 3...) trùng
	 * với label trục X trong FXML là "Lượt bid". Trước đây realtime dùng "số giây
	 * kể từ lúc mở view" còn historical dùng "chỉ số bid", nên đường biểu đồ bị
	 * gãy/lùi về 0 mỗi lần có bid mới.
	 */
	private void appendChartPoint(double amount) {

		if (priceSeries == null) {
			return;
		}

		// bidCount chuẩn bị được tăng ở dưới; X của điểm mới = bidCount + 1
		int xIndex = priceSeries.getData().size(); // tiếp nối ngay sau điểm cuối hiện có

		priceSeries.getData().add(new XYChart.Data<>(xIndex, amount));

		// chống memory leak chart
		if (priceSeries.getData().size() > 200) {
			priceSeries.getData().remove(0);
		}

		// update thống kê
		bidCount++;

		if (amount > peakPrice) {
			peakPrice = amount;
		}

		if (lblBidCount != null) {
			lblBidCount.setText(String.valueOf(bidCount));
		}

		if (lblPeakPrice != null) {
			lblPeakPrice.setText(formatMoney(peakPrice));
		}
	}

	// =====================================================================
	// PLACE BID
	// =====================================================================

	@FXML
	private void handlePlaceBid() {
		if (currentAuctionId == null) {
			showMessage("Chưa có phiên đấu giá để đặt giá.");
			return;
		}
		if (!Session.get().isLoggedIn()) {
			showMessage("Bạn cần đăng nhập trước.");
			return;
		}

		String amountText = txtBidAmount != null ? txtBidAmount.getText().trim() : "";
		if (amountText.isEmpty()) {
			showMessage("Vui lòng nhập số tiền đấu giá.");
			return;
		}

		try {
			double amount = Double.parseDouble(amountText);
			BidRequest req = new BidRequest();
			req.setAuctionId(currentAuctionId);
			req.setAmount(amount);
			req.setUserId(Session.get().getUserId());

			if (btnPlaceBid != null) {
				btnPlaceBid.setDisable(true);
			}

			new Thread(() -> {
				String resStr = NetworkClient.getInstance().sendAndReceive("PLACE_BID", req);
				Platform.runLater(() -> {
					if (btnPlaceBid != null) {
						btnPlaceBid.setDisable(false);
					}

					if (resStr == null || resStr.isBlank()) {
						showMessage("Lỗi: Không nhận được phản hồi từ máy chủ.");
						return;
					}

					try {
						JsonObject resObj = JsonParser.parseString(resStr).getAsJsonObject();
						boolean success = resObj.has("success") && resObj.get("success").getAsBoolean();
						String message = getStr(resObj, "message",
								success ? "Đặt giá thành công!" : "Đặt giá thất bại.");

						if (lblBidResult != null) {
							lblBidResult.setText((success ? "✅ " : "❌ ") + message);
						}

						if (success) {
							if (txtBidAmount != null) {
								txtBidAmount.clear();
							}
							if (lblCurrent != null && resObj.has("currentPrice")) {
								lblCurrent.setText(formatMoney(getDouble(resObj, "currentPrice")));
							}
							// KHÔNG cần update chart tại đây — broadcast AUCTION_UPDATE
							// sẽ về và onAuctionUpdate() sẽ làm việc đó.
						}
					} catch (Exception ex) {
						showMessage("Lỗi đồng bộ dữ liệu mạng hệ thống.");
					}
				});
			}).start();
		} catch (NumberFormatException ex) {
			showMessage("Số tiền nhập vào không đúng định dạng số hợp lệ.");
		}
	}

	@FXML
	private void handleQuickAdd10k() {
		quickAdd(10_000);
	}

	@FXML
	private void handleQuickAdd50k() {
		quickAdd(50_000);
	}

	@FXML
	private void handleQuickAdd100k() {
		quickAdd(100_000);
	}

	private void quickAdd(double delta) {
		double base = currentPriceValue();
		double newValue = base + delta;
		if (txtBidAmount != null) {
			txtBidAmount.setText(String.format(Locale.US, "%.0f", newValue));
		}
	}

	private double currentPriceValue() {
		// Nếu user đang gõ thì cộng dồn vào số đó, không thì lấy giá hiện tại
		if (txtBidAmount != null && !txtBidAmount.getText().isBlank()) {
			try {
				return Double.parseDouble(txtBidAmount.getText().trim());
			} catch (NumberFormatException ignored) {
				/* fall through */ }
		}
		if (lblCurrent != null) {
			String s = lblCurrent.getText().replaceAll("[^0-9]", "");
			if (!s.isEmpty()) {
				try {
					return Double.parseDouble(s);
				} catch (NumberFormatException ignored) {
					/* fall through */ }
			}
		}
		return 0.0;
	}

	// =====================================================================
	// NAVIGATION
	// =====================================================================

	@FXML
	private void handleBack() {
		destroy();
		if (mainWindowController != null) {
			mainWindowController.showBrowseAuctions();
		}
	}

	// =====================================================================
	// REALTIME (Observer of AUCTION_UPDATE broadcast)
	// =====================================================================

	@Override
	public void onAuctionUpdate(JsonObject data) {
		if (data == null || currentAuctionId == null || !data.has("auctionId")) {
			return;
		}

		if (data.get("auctionId").getAsLong() != currentAuctionId.longValue()) {
			return;
		}

		double newPrice = data.has("currentPrice") ? data.get("currentPrice").getAsDouble() : 0.0;
		String newStatus = getStr(data, "status", null);
		String newEndTime = getStr(data, "endTime", lblEnd != null ? lblEnd.getText() : "");
		String msg = getStr(data, "message", "Cập nhật từ Server!");
		String bidderName = getStr(data, "bidderName", "Đối thủ");
		String bidTime = getStr(data, "bidTime", "Vừa xong");
		String bidId = getStr(data, "bidId", "");

		Platform.runLater(() -> {
			// Cập nhật giá nếu có bid mới
			if (newPrice > 0 && bidId != null && !bidId.isEmpty()) {
				if (lblCurrent != null) {
					lblCurrent.setText(formatMoney(newPrice));
				}
				// SỬA: chỉ rebuild countdown khi endTime thực sự thay đổi
				// -> tránh giật label mỗi khi có bid liên tiếp.
				if (lblEnd != null && newEndTime != null && !newEndTime.isBlank()) {
					String old = lblEnd.getText();
					if (!newEndTime.equals(old)) {
						lblEnd.setText(newEndTime);
						setupCountdown(newEndTime);
					}
				}

				// 1) Thêm dòng vào bảng (đầu danh sách)
				bidsList.add(0, new BidRow(bidId, bidderName, newPrice, bidTime));

				// 2) 📈 Thêm điểm mới vào biểu đồ realtime
				appendChartPoint(newPrice);

				// 3) Cập nhật label thống kê + bidder cao nhất
				if (newPrice > peakPrice)
					peakPrice = newPrice;
				updateBidStats();
				updateHighestBidderLabel();

				if (lblBidResult != null) {
					lblBidResult.setText("🔔 " + msg + " (" + bidderName + " - " + formatMoney(newPrice) + ")");
				}

				// Flash đèn LIVE
				flashLive();
			}
			// Cập nhật trạng thái nếu có (từ status monitor)
			else if (newStatus != null && !newStatus.isEmpty()) {
				applyStatusStyle(newStatus);
				System.out.println(
						"[AuctionDetailViewController] Trạng thái phiên #" + currentAuctionId + " -> " + newStatus);

				// Hiển thị thông báo trạng thái
				if (lblBidResult != null) {
					if ("FINISHED".equals(newStatus) || "FINISHED".equals(newStatus.toUpperCase())) {
						lblBidResult.setText("⏹️ Phiên đấu giá đã kết thúc!");
						if (btnPlaceBid != null) {
							btnPlaceBid.setDisable(true);
						}
					} else if ("RUNNING".equals(newStatus) || "RUNNING".equals(newStatus.toUpperCase())) {
						lblBidResult.setText("🎯 Phiên đấu giá đang diễn ra...");
						if (btnPlaceBid != null) {
							btnPlaceBid.setDisable(false);
						}
					} else {
						lblBidResult.setText("📋 Trạng thái: " + newStatus);
					}
				}

				// Flash đèn LIVE
				flashLive();
			}
		});
	}

	/**
	 * Thêm 1 điểm dữ liệu mới vào line chart (giờ thực tế tính bằng giây).
	 */

	private void updateBidStats() {
		if (lblBidCount != null) {
			lblBidCount.setText(bidCount + " bids");
		}
		if (lblPeakPrice != null) {
			lblPeakPrice.setText("Peak: " + formatMoney(peakPrice));
		}
	}

	private void updateHighestBidderLabel() {
		if (lblHighestBidder == null)
			return;
		if (bidsList.isEmpty()) {
			lblHighestBidder.setText("👑 Cao nhất: -");
			return;
		}
		BidRow top = bidsList.get(0);
		lblHighestBidder.setText(
				"👑 Cao nhất: " + top.bidderProperty().get() + " (" + formatMoney(top.amountProperty().get()) + ")");
	}

	// =====================================================================
	// COUNTDOWN
	// =====================================================================

	private void setupCountdown(String endTimeStr) {
		if (lblCountdown == null)
			return;

		// Cố parse string ISO
		endTime = null;
		if (endTimeStr != null && !endTimeStr.isBlank() && !"-".equals(endTimeStr)) {
			try {
				endTime = LocalDateTime.parse(endTimeStr, ISO_FORMATTER);
			} catch (Exception ignored) {
				try {
					// Backup: chấp nhận định dạng có thể là "yyyy-MM-dd HH:mm:ss"
					DateTimeFormatter alt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
					endTime = LocalDateTime.parse(endTimeStr, alt);
				} catch (Exception ignored2) {
					endTime = null;
				}
			}
		}

		// Stop timeline cũ
		if (countdownTimeline != null) {
			countdownTimeline.stop();
		}

		if (endTime == null) {
			lblCountdown.setText("--:--:--");
			return;
		}

		countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateCountdownLabel()));
		countdownTimeline.setCycleCount(Timeline.INDEFINITE);
		countdownTimeline.play();
		updateCountdownLabel();
	}

	private void updateCountdownLabel() {
		if (lblCountdown == null || endTime == null)
			return;
		LocalDateTime now = LocalDateTime.now();
		long seconds = ChronoUnit.SECONDS.between(now, endTime);
		if (seconds <= 0) {
			lblCountdown.setText("⌛ Đã kết thúc");
			lblCountdown.getStyleClass().removeAll("countdown-label", "countdown-urgent");
			lblCountdown.getStyleClass().add("countdown-ended");
			if (countdownTimeline != null)
				countdownTimeline.stop();
			return;
		}
		long h = seconds / 3600;
		long m = (seconds % 3600) / 60;
		long s = seconds % 60;
		lblCountdown.setText(String.format("%02d:%02d:%02d", h, m, s));

		// Dưới 60 giây -> chuyển sang style cảnh báo
		lblCountdown.getStyleClass().removeAll("countdown-urgent", "countdown-ended");
		if (!lblCountdown.getStyleClass().contains("countdown-label")) {
			lblCountdown.getStyleClass().add("countdown-label");
		}
		if (seconds < 60) {
			lblCountdown.getStyleClass().add("countdown-urgent");
		}
	}

	// =====================================================================
	// LIVE indicator blink
	// =====================================================================

	private void startLiveBlink() {
		if (lblLiveIndicator == null)
			return;
		liveBlinkTimeline = new Timeline(new KeyFrame(Duration.seconds(0), e -> lblLiveIndicator.setOpacity(1.0)),
				new KeyFrame(Duration.seconds(0.6), e -> lblLiveIndicator.setOpacity(0.35)),
				new KeyFrame(Duration.seconds(1.2), e -> lblLiveIndicator.setOpacity(1.0)));
		liveBlinkTimeline.setCycleCount(Timeline.INDEFINITE);
		liveBlinkTimeline.play();
	}

	private void flashLive() {
		if (lblLiveIndicator == null)
			return;
		lblLiveIndicator.setOpacity(1.0);
	}

	// =====================================================================
	// STATUS pill style
	// =====================================================================

	private void applyStatusStyle(String status) {
		if (lblStatus == null)
			return;
		lblStatus.setText(status);
		lblStatus.getStyleClass().removeAll("status-open", "status-closed", "status-end");
		String s = status == null ? "" : status.toUpperCase(Locale.ROOT);
		if (s.contains("OPEN")) {
			lblStatus.getStyleClass().add("status-open");
		} else if (s.contains("CLOSE")) {
			lblStatus.getStyleClass().add("status-closed");
		} else {
			lblStatus.getStyleClass().add("status-end");
		}
	}

	// =====================================================================
	// CLEAN UP
	// =====================================================================

	public void destroy() {
		NetworkClient.getInstance().removeListener(this);
		if (countdownTimeline != null) {
			countdownTimeline.stop();
			countdownTimeline = null;
		}
		if (liveBlinkTimeline != null) {
			liveBlinkTimeline.stop();
			liveBlinkTimeline = null;
		}
	}

	// =====================================================================
	// HELPERS
	// =====================================================================

	private void showMessage(String message) {
		if (lblBidResult != null) {
			lblBidResult.setText(message);
		}
	}

	private static String formatMoney(double value) {
		return String.format(Locale.US, "%,.0f ₫", value);
	}

	private static String getStr(JsonObject o, String k, String fallback) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : fallback;
	}

	private static double getDouble(JsonObject o, String k) {
		return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsDouble() : 0.0;
	}

	public static class BidRow {
		private final SimpleStringProperty id;
		private final SimpleStringProperty bidder;
		private final SimpleDoubleProperty amount;
		private final SimpleStringProperty time;

		public BidRow(String id, String bidder, double amount, String time) {
			this.id = new SimpleStringProperty(id);
			this.bidder = new SimpleStringProperty(bidder == null ? "" : bidder);
			this.amount = new SimpleDoubleProperty(amount);
			this.time = new SimpleStringProperty(time);
		}

		public SimpleStringProperty idProperty() {
			return id;
		}

		public SimpleStringProperty bidderProperty() {
			return bidder;
		}

		public SimpleDoubleProperty amountProperty() {
			return amount;
		}

		public SimpleStringProperty timeProperty() {
			return time;
		}
	}
}
