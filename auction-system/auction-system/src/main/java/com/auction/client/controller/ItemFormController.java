package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.dto.ItemRequest;
import com.auction.dto.ItemResponse;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class ItemFormController {
	@FXML
	private TextField txtName, txtPrice;
	@FXML
	private TextArea txtDesc;
	@FXML
	private ComboBox<String> cbCategory;

	private Long currentId = null;
	private Gson gson = new Gson();

	@FXML
	public void handleSave() {
		ItemRequest req = new ItemRequest();
		req.setItemId(this.currentId); // Gửi ID hiện tại (null nếu là thêm mới)
		req.setName(txtName.getText());
		req.setDescription(txtDesc.getText());
		req.setType(cbCategory.getValue());

		try {
			req.setStartingPrice(Double.parseDouble(txtPrice.getText()));
		} catch (Exception e) {
			showAlert("Giá khởi điểm không hợp lệ!");
			return;
		}

		req.setSellerId(1L);

		String resJson = NetworkClient.getInstance().sendAndReceive("SAVE_ITEM", req);

		if (resJson != null && !resJson.equals("null")) { // Kiểm tra chuỗi trả về
			ItemResponse res = gson.fromJson(resJson, ItemResponse.class);

			// KIỂM TRA AN TOÀN: res phải khác null mới được gọi hàm
			if (res != null) {
				showAlert(res.getMessage());
				if (res.isSuccess() && res.getItemData() != null) {
					this.currentId = res.getItemData().getItemId();
				}
			} else {
				showAlert("Lỗi: Phản hồi từ Server không hợp lệ.");
			}
		} else {
			showAlert("Lỗi: Không nhận được phản hồi từ Server.");
		}
	}

	private void showAlert(String msg) {
		Alert alert = new Alert(Alert.AlertType.INFORMATION);
		alert.setContentText(msg);
		alert.show();
	}
}