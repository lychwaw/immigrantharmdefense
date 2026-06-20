package ui;
import java.util.List;

import data.JSONLoader;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import model.Persona;
import model.Scenario;
import workingengine.ClaudeConnector;
import workingengine.ConvoEngine;
import workingengine.OpenRouterConnector;

public class RunSession { // all of these will be placed on the UI to ensure the testing of the model is simple to use

   private ComboBox<String> personaComboBox;
    private ComboBox<String> scenarioComboBox;
    private ComboBox<String> modeComboBox;
    private ComboBox<String> modelComboBox;
    private Button startBtnButton;
    private TextArea convoArea;
    private TextField inputField;
    private Button sendBtn;
    private ConvoEngine engine;

    private ConvoEngine createEngineForModelChoice(String modelChoice) {
        switch (modelChoice) {
            case "Llama 3.2 3B (OpenRouter)":
                return new ConvoEngine(new OpenRouterConnector(), "meta-llama/llama-3.2-3b-instruct:free");
            case "Claude Sonnet 4.6":
            default:
                return new ConvoEngine(new ClaudeConnector(), "claude-sonnet-4-6");
        }
    }

    public VBox getContent(){
        VBox layout = new VBox(10);
        try{
            List<Persona> personas = JSONLoader.loadPersonas(); // lists created with JSON that parses the Json files made
            List<Scenario> scenarios = JSONLoader.loadScenarios();

            personaComboBox = new ComboBox<>(FXCollections.observableArrayList(
                personas.stream().map(p -> p.name).toArray(String[]::new)
            ));
            scenarioComboBox = new ComboBox<>(FXCollections.observableArrayList(
                scenarios.stream().map(s -> s.domain).toArray(String[]::new)
            ));
            modeComboBox = new ComboBox<>(FXCollections.observableArrayList("Baseline", "Model")); // three dropdowns are created for the user to select the persona, scenario and mode (baseline or model) they want to test with
            modelComboBox = new ComboBox<>(FXCollections.observableArrayList("Claude Sonnet 4.6", "Llama 3.2 3B (OpenRouter)"));
            modelComboBox.setValue("Claude Sonnet 4.6");
            startBtnButton = new Button("Start a new session"); // objects created for start, convo area, where the input goes and finally the send button
            convoArea = new TextArea();
            convoArea.setEditable(false);
            inputField = new TextField();
            sendBtn = new Button("Send");
            sendBtn.setDisable(true); // only appears once a session is ran

            startBtnButton.setOnAction(e -> {
                String personaName = personaComboBox.getValue();
                String scenarioDomain = scenarioComboBox.getValue();
                String mode = modeComboBox.getValue();
                String modelChoice = modelComboBox.getValue();
                if (personaName == null || scenarioDomain == null || mode == null || modelChoice == null) {
                    convoArea.appendText("Please select a persona, scenario, mode, and model first.\n");
                    return;
                }
                Persona selectedPersona = personas.stream().filter(p -> p.name.equals(personaName)).findFirst().orElse(null);
                Scenario selectedScenario = scenarios.stream().filter(s -> s.domain.equals(scenarioDomain)).findFirst().orElse(null);
                if (selectedPersona == null || selectedScenario == null) {
                    convoArea.appendText("Could not find the selected persona/scenario in the loaded data.\n");
                    return;
                }

                engine = createEngineForModelChoice(modelChoice);
                startBtnButton.setDisable(true);
                convoArea.appendText("Waiting for response... (may take up to a minute if rate-limited)\n");

                Task<String> task = new Task<>() {
                    @Override
                    protected String call() {
                        return engine.startSession(selectedPersona, selectedScenario, mode);
                    }
                };
                task.setOnSucceeded(evt -> {
                    convoArea.appendText("AI: " + task.getValue() + "\n");
                    startBtnButton.setDisable(false);
                    sendBtn.setDisable(false);
                });
                task.setOnFailed(evt -> {
                    convoArea.appendText("AI: Error: " + task.getException().getMessage() + "\n");
                    startBtnButton.setDisable(false);
                });
                Thread thread = new Thread(task);
                thread.setDaemon(true);
                thread.start();
            });

            sendBtn.setOnAction(e -> {
                String userMessage = inputField.getText();
                if (userMessage == null || userMessage.isBlank()) return;
                convoArea.appendText("User: " + userMessage + "\n");
                inputField.clear();
                sendBtn.setDisable(true);
                convoArea.appendText("Waiting for response... (may take up to a minute if rate-limited)\n");

                Task<String> task = new Task<>() {
                    @Override
                    protected String call() {
                        return engine.sendUserMessage(userMessage);
                    }
                };
                task.setOnSucceeded(evt -> {
                    convoArea.appendText("AI: " + task.getValue() + "\n");
                    sendBtn.setDisable(false);
                });
                task.setOnFailed(evt -> {
                    convoArea.appendText("AI: Error: " + task.getException().getMessage() + "\n");
                    sendBtn.setDisable(false);
                });
                Thread thread = new Thread(task);
                thread.setDaemon(true);
                thread.start();
            });

            layout.getChildren().addAll(new Label("Select a persona:"), personaComboBox,  // container box holds the child items
                                        new Label("Select a scenario:"), scenarioComboBox,
                                        new Label("Select a mode:"), modeComboBox,
                                        new Label("Select a model:"), modelComboBox,
                                        startBtnButton, convoArea, inputField, sendBtn);  // adds all lists to the overall layout in the correct order
        } catch (Exception e){
            layout.getChildren().add(new Label("Error loading data: " + e.getMessage()));
        }
        return layout;
    }
}