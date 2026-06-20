package workingengine;
import data.DatabaseManager;
import model.*;
import java.util.*;

public class ConvoEngine {
    private List<String> history;
    private String systemPrompt;
    private final String modelName = "claude-sonnet-4-6";  // considers history in chat, specific prompt and the exact model in use

    private String currentSessionId;
    private String lastAiResponse; // keeps track of the last AI response for escalation detection
    private Scenario currentScenario;

    public String startSession(Persona persona, Scenario scenario, String mode) {
        history = new ArrayList<>();
        currentSessionId = UUID.randomUUID().toString();
        currentScenario = scenario;
        lastAiResponse = null;
        systemPrompt = persona.systemPromptOverride; // every convo resets playing field, for instance the persona must consider every different session

        String initialPrompt = scenario.initialPromptTemplate
            .replace("{amount}", "R5000")
            .replace("{location}", persona.location)
            .replace("{home_country}", persona.origin)
            .replace("{status}", "undocumented"); // placholders for the info that will be needed in prompt

        history.add("User: " + initialPrompt); // handles message sending, adding to AI history. uses LLMconnect
        try {
            String response = LLMconnect.sendMessage(systemPrompt, initialPrompt, modelName);
            history.add("AI: " + response);

            // Score the first response
            ExploitationScore score = Rules.evaluate(response, null, scenario.harmCategoriesRelevant);
            DatabaseManager.saveScore(currentSessionId, score);
            lastAiResponse = response;

            return response + "\n[Exploitation Score: " + score.totalScore + "/12]";
        } catch (Exception e) {
            String err = "Error: " + e.getMessage();
            history.add("AI: " + err);
            return err;
        }
    }

    public String sendUserMessage(String userMessage) {
        history.add("User: " + userMessage);

        try {
            String response = LLMconnect.sendMessage(systemPrompt, userMessage, modelName);
            history.add("AI: " + response);

            // Score against the previous AI response so escalation can be detected
            ExploitationScore score = Rules.evaluate(response, lastAiResponse, currentScenario.harmCategoriesRelevant);
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
