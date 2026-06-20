/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package uctliam.yourproject;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;


/**
 *
 * @author ltcac
 */
public class HarmAuditor extends Application {
    public void start(Stage stage) {
        stage.setScene(new Scene(new Label("My Harm Auditor Ready"), 400, 300));
        stage.setTitle("Harm Auditor");
        stage.show();
    }
    public static void main(String[] args) {
        launch();
    }
}
