package ui;
import java.util.List;

import data.JSONLoader;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import model.Persona;
import workingengine.BatchRun;
import workingengine.ClaudeConnector;
import workingengine.ConvoEngine;
import workingengine.GeminiConnector;

public class RunSession { // all of these will be placed on the UI to ensure the testing of the model is simple to use, fully automated now

    private ComboBox<String> personaComboBox;
    private ComboBox<String> modeComboBox;
    private ComboBox<String> modelComboBox;
    private TextArea convoArea;
    private Label statusLabel;

    private ConvoEngine createEngineForModelChoice(String modelChoice) {
        switch (modelChoice) {
            case "Gemini 2.5 Flash Lite":
                return new ConvoEngine(new GeminiConnector(), "gemini-2.5-flash-lite");
            case "Claude Sonnet 4.6":
            default:
                return new ConvoEngine(new ClaudeConnector(), "claude-sonnet-4-6");
        }
    }

    public VBox getContent(){
        VBox layout = new VBox(10);
        try{
            List<Persona> personas = JSONLoader.loadPersonas(); // lists created with JSON that parses the Json files made

            personaComboBox = new ComboBox<>(FXCollections.observableArrayList(
                personas.stream().map(p -> p.name).toArray(String[]::new)
            ));
            modeComboBox = new ComboBox<>(FXCollections.observableArrayList("Baseline", "Model")); // mode (baseline or model) for the batch run
            modelComboBox = new ComboBox<>(FXCollections.observableArrayList("Claude Sonnet 4.6", "Gemini 2.5 Flash Lite"));
            modelComboBox.setValue("Claude Sonnet 4.6");
            convoArea = new TextArea();
            convoArea.setEditable(false);
            statusLabel = new Label("Status: Idle");

            Button batchBtn = new Button("Run All Sessions");
            batchBtn.setOnAction(e -> { // when the batch button is clicked, the batch run is executed and the results are displayed in the conversation area
                String personaName = personaComboBox.getValue();
                Persona selectedPersona = personaName == null ? null
                    : personas.stream().filter(p -> p.name.equals(personaName)).findFirst().orElse(null);
                String mode = modeComboBox.getValue() == null ? "Model" : modeComboBox.getValue();
                String modelChoice = modelComboBox.getValue() == null ? "Claude Sonnet 4.6" : modelComboBox.getValue();

                convoArea.clear();
                convoArea.appendText("Running automated " + mode + " batch (" + modelChoice + ") for "
                    + (selectedPersona == null ? "all personas" : selectedPersona.name) + "...\n");
                batchBtn.setDisable(true);
                statusLabel.setText("Status: Running batch (" + mode + " mode)...");

                Task<String> batchTask = new Task<>() {
                    @Override
                    protected String call() {
                        return BatchRun.runAll(6, selectedPersona, mode,
                            () -> createEngineForModelChoice(modelChoice),
                            line -> Platform.runLater(() ->
                                statusLabel.setText("Status: Running batch (" + mode + " mode) — " + line.trim()))
                        );
                    }
                };
                batchTask.setOnSucceeded(evt -> {
                    convoArea.appendText(batchTask.getValue());
                    statusLabel.setText("Status: Batch complete (" + mode + " mode)");
                    batchBtn.setDisable(false);  // if succeeds, returns results. if not, returns error below
                });
                batchTask.setOnFailed(evt -> {
                    convoArea.appendText("Batch error: " + batchTask.getException().getMessage() + "\n");
                    statusLabel.setText("Status: Batch error (" + mode + " mode) — see above");
                    batchBtn.setDisable(false);
                });
                Thread batchThread = new Thread(batchTask);
                batchThread.setDaemon(true);
                batchThread.start();
            });

            layout.getChildren().addAll(new Label("Select a persona:"), personaComboBox,  // container box holds the child items
                                        new Label("Select a mode:"), modeComboBox,
                                        new Label("Select a model:"), modelComboBox,
                                        batchBtn, statusLabel, convoArea);  // adds all lists to the overall layout in the correct order
        } catch (Exception e){
            layout.getChildren().add(new Label("Error loading data: " + e.getMessage()));
        }
        return layout;
    }
}
