package com.auction.clientapp;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class ClientApp extends Application {
	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainWindow.fxml"));
		Scene scene = new Scene(root, 1180, 720);
		stage.setScene(scene);
		stage.setTitle("Hệ Thống Đấu Giá – Nhóm 14");
		stage.setMinWidth(960);
		stage.setMinHeight(620);
		stage.show();
	}
}
