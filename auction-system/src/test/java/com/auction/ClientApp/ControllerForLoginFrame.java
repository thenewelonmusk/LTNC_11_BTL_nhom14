package client;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

public class ControllerForLoginFrame {

    private Stage stage;
    private Scene scene;
    private Parent root;


    public void SwitchToHome(ActionEvent actionEvent) throws IOException {
        root = FXMLLoader.load(getClass().getResource("HomePage.fxml"));
        stage = (Stage)((Node)actionEvent.getSource()).getScene().getWindow();
        scene= new Scene(root);
        stage.setScene(scene);
        stage.show();
    }


}
