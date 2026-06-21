package workingengine;
import data.DatabaseManager;
import model.*;
import java.time.Instant;
import java.util.*;

public class ConvoEngine {
    private List<String> history;
    private String systemPrompt;
    private final LLMProvider llmProvider;
    private final String modelName;
    private final LLMProvider judgeProvider;
    private final String judgeModel;

    private String currentSessionId;
    private String lastAiResponse; // last AI reply, fed to CreatePersonaSim so the simulated persona can react to it
    private Scenario currentScenario;
    private int turnCounter;

    public ConvoEngine() {
        this(new ClaudeConnector(), "claude-sonnet-4-6");
    }

    public ConvoEngine(LLMProvider llmProvider, String modelName) {
        this.llmProvider = llmProvider;
        this.modelName = modelName;
        // judge is always the OTHER model, so the model under test never grades its own output
        if (modelName.toLowerCase().contains("claude")) {
            this.judgeProvider = new GeminiConnector();
            this.judgeModel = "gemini-2.5-flash-lite";
        } else {
            this.judgeProvider = new ClaudeConnector();
            this.judgeModel = "claude-sonnet-4-6";
        }
    }

    public String startSession(Persona persona, Scenario scenario, String mode) {
        history = new ArrayList<>();
        currentSessionId = UUID.randomUUID().toString();
        currentScenario = scenario;
        lastAiResponse = null;
        turnCounter = 0;

        String initialPrompt;
        if ("Baseline".equals(mode)) {
            // Baseline = control: no persona manipulation, just the neutral version of the same topic
            systemPrompt = "";
            initialPrompt = scenario.baselinePrompt;
        } else {
            systemPrompt = persona.systemPromptOverride; // every convo resets playing field, for instance the persona must consider every different session
            initialPrompt = scenario.initialPromptTemplate
                .replace("{amount}", "R5000")
                .replace("{location}", persona.location)
                .replace("{home_country}", persona.origin)
                .replace("{status}", "undocumented"); // placholders for the info that will be needed in prompt
        }

        DatabaseManager.saveSession(currentSessionId, persona.id, scenario.id, mode, Instant.now().toString());

        history.add("User: " + initialPrompt); // handles message sending, adding to AI history. uses the configured llmProvider
        DatabaseManager.saveTurn(currentSessionId, ++turnCounter, "User", initialPrompt, Instant.now().toString());
        try {
            String response = llmProvider.sendMessage(systemPrompt, initialPrompt, modelName);
            history.add("AI: " + response);
            DatabaseManager.saveTurn(currentSessionId, ++turnCounter, "AI", response, Instant.now().toString());

            // Score the first response
            ExploitationScore score = Rules.evaluate(initialPrompt, response, judgeProvider, judgeModel);
            DatabaseManager.saveScore(currentSessionId, score);
            lastAiResponse = response;

            return response + "\n[Exploitation Score: " + score.totalScore + "/12]";
        } catch (Exception e) {
            String err = "Error: " + e.getMessage();
            history.add("AI: " + err);
            return err;
        }
    }

    public void runAutomatedSession(Persona persona, Scenario scenario, int totalTurns, String mode) {
        startSession(persona, scenario, mode);
        for (int i = 1; i < totalTurns; i++) {
            String simulatedUserMessage = CreatePersonaSim.generateResponse(llmProvider, modelName, persona, lastAiResponse, history);
            sendUserMessage(simulatedUserMessage);
        }
    }

    public String sendUserMessage(String userMessage) {
        history.add("User: " + userMessage);
        DatabaseManager.saveTurn(currentSessionId, ++turnCounter, "User", userMessage, Instant.now().toString());

        try {
            String response = llmProvider.sendMessage(systemPrompt, userMessage, modelName);
            history.add("AI: " + response);
            DatabaseManager.saveTurn(currentSessionId, ++turnCounter, "AI", response, Instant.now().toString());

            ExploitationScore score = Rules.evaluate(userMessage, response, judgeProvider, judgeModel);
            DatabaseManager.saveScore(currentSessionId, score);
            lastAiResponse = response;

            return response + "\n[Exploitation Score: " + score.totalScore + "/12]";
        } catch (Exception e) {
            String err = "Error: " + e.getMessage();
            history.add("AI: " + err);
            return err;
        }
    }
}
