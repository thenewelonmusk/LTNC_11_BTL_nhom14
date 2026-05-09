package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HomePageController {
    private Stage stage;
    private Scene scene;
    private Parent root;
    public void ToAuctionFrame(ActionEvent actionEvent) throws IOException {
        root = FXMLLoader.load(getClass().getResource("AuctionFrame.fxml"));
        stage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
        scene= new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void ToHostingFrame(ActionEvent actionEvent) throws IOException {
        root = FXMLLoader.load(getClass().getResource("HostingFrame.fxml"));
        stage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
        scene= new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void ToItemFrame(ActionEvent actionEvent) throws IOException {
        root = FXMLLoader.load(getClass().getResource("ItemFrame.fxml"));
        stage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
        scene= new Scene(root);
        stage.setScene(scene);
        stage.show();
    }

    public void back(ActionEvent actionEvent) throws IOException{
        root = FXMLLoader.load(getClass().getResource("loginFrame.fxml"));
        stage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
        scene= new Scene(root);
        stage.setScene(scene);
        stage.show();
    }
}
