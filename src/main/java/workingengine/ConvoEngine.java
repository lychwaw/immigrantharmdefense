package workingengine;
import model.*;
import java.util.*;

public class ConvoEngine {
    private List<String> history;
    private String systemPrompt;
    private final String modelName = "claude-sonnet-4-6";

    public String startSession(Persona persona, Scenario scenario, String mode) {
        history = new ArrayList<>();
        systemPrompt = persona.sysOverride;
        // Build initial user prompt by replacing template variables
        String initialPrompt = scenario.initialPromptDraft
            .replace("{amount}", "R5000")
            .replace("{location}", persona.location)
            .replace("{home_country}", persona.origin)
            .replace("{status}", "undocumented");
        // For baseline, you can branch later; for now just use initialPrompt
        history.add("User: " + initialPrompt);
        try {
            String response = LLMconnect.sendMessage(systemPrompt, initialPrompt, modelName);
            history.add("AI: " + response);
            return response;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    public String sendUserMessage(String userMessage) {
        history.add("User: " + userMessage);
        // Convert history to messages array; for simplicity, concatenate previous exchanges into the user message context
        // Better approach: build full message list from history. We'll do minimal for now: send the last user message with system prompt.
        try {
            String response = LLMconnect.sendMessage(systemPrompt, userMessage, modelName);
            history.add("AI: " + response);
            return response;
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
