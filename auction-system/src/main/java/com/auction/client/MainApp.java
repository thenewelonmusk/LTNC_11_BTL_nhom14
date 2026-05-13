package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Đảm bảo bạn đã có file Login.fxml trong thư mục resources
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/TestDashboard.fxml"));
        primaryStage.setTitle("Hệ Thống Đấu Giá Lập Trình Nâng Cao");
        primaryStage.setScene(new Scene(root, 400, 300));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}