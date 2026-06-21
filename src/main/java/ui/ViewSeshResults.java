package ui;

import data.DatabaseManager;
import javafx.collections.FXCollections;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.List;
import java.util.Map;

public class ViewSeshResults { // this is the tab where we can see the results of the sessions, including summaries and worst sessions with transcripts
    private TextArea summaryArea;
    private TextArea transcriptArea;
    private ComboBox<String> worstSessionCombo;
    private List<String[]> worstSessions; // store session IDs

    public VBox getContent() {
        VBox vbox = new VBox(10);

        Button refreshBtn = new Button("Refresh Data");
        summaryArea = new TextArea();
        summaryArea.setEditable(false);
        summaryArea.setPrefHeight(200);

        Label worstLabel = new Label("Worst Sessions (click to view transcript):");
        worstSessionCombo = new ComboBox<>();
        worstSessionCombo.setPrefWidth(400);
        transcriptArea = new TextArea();
        transcriptArea.setEditable(false);
        transcriptArea.setPrefHeight(250); // my GUI constraints

        refreshBtn.setOnAction(e -> loadData()); // event handler to reload data and transcropt with selected buttons
        worstSessionCombo.setOnAction(e -> loadTranscript());

        vbox.getChildren().addAll(refreshBtn, new Label("Summary:"), summaryArea,
                worstLabel, worstSessionCombo, transcriptArea);
        return vbox;
    }

    private void loadData() {
        // Summary
        StringBuilder sb = new StringBuilder();

        Map<String, Double> byPersona = DatabaseManager.getMeanScoresByPersona();  // map collection view for our data
        sb.append("Mean Exploitation Score by Persona:\n");
        for (Map.Entry<String, Double> entry : byPersona.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(String.format("%.1f", entry.getValue())).append("/12\n");
        }

        Map<String, Double> byScenario = DatabaseManager.getMeanScoresByScenario();
        sb.append("\nMean Score by Scenario:\n");
        for (Map.Entry<String, Double> entry : byScenario.entrySet()) {
            sb.append("  ").append(entry.getKey()).append(": ").append(String.format("%.1f", entry.getValue())).append("/12\n");
        }

        summaryArea.setText(sb.toString());

        // worst sessions dropdown
        worstSessions = DatabaseManager.getWorstSessions(5);  // display array list of worst sessions
        worstSessionCombo.setItems(FXCollections.observableArrayList(
            worstSessions.stream()
                .map(arr -> arr[1] + " x " + arr[2] + " (Score: " + arr[3] + ")") // arrays for session id, persona name, scenario name, score
                .toList()
        ));
    }

    private void loadTranscript() {
        int idx = worstSessionCombo.getSelectionModel().getSelectedIndex(); //selected index of worst session is displayed
        if (idx >= 0 && idx < worstSessions.size()) {
            String sessionId = worstSessions.get(idx)[0];
            String transcript = DatabaseManager.getTranscript(sessionId);
            transcriptArea.setText(transcript);
        } 
    }
}