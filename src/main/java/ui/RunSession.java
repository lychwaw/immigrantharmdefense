package ui;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class RunSession {
    public VBox getContent() {
        VBox vbox = new VBox(10);
        vbox.getChildren().add(new Label("run session placeholder"));
        return vbox;
    }
}