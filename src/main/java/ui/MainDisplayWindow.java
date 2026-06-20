package ui;
import data.DatabaseManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

public class MainDisplayWindow extends Application {  // now this window will open the tabs we need to see whatever is needed

    @Override
    public void start(Stage stage) {
        DatabaseManager.initialiseDB();

        TabPane tabPane = new TabPane(); 

        Tab runTab = new Tab("Run Session"); // session tab of course, for running sessions
        runTab.setContent(new RunSession().getContent());
        runTab.setClosable(false);

        Tab resultsTab = new Tab("View Results"); // results tab to see what data we get from the sessions
        resultsTab.setContent(new ViewSeshResults().getContent());
        resultsTab.setClosable(false);

        Tab reportTab = new Tab("Generate Report"); // finally , i can generate an overall report for the sessions
        reportTab.setContent(new GenerateReport().getContent());
        reportTab.setClosable(false);

        tabPane.getTabs().addAll(runTab, resultsTab, reportTab);

        stage.setScene(new Scene(tabPane, 800, 600));
        stage.setTitle("Red-Team Immigrant Danger Simulator");
        stage.show();  
    }

    public static void main(String[] args) {
        launch();
    }
}