package com.auction.clientapp;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ItemFrameController {
	private Stage stage;
	private Scene scene;
	private Parent root;

	public void back(ActionEvent actionEvent) throws IOException {
		root = FXMLLoader.load(getClass().getResource("HomePage.fxml"));
		stage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
		scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
	}
}
