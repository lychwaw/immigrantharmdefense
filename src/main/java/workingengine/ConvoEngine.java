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
    private final LLMProvider personaSimProvider;
    private final String personaSimModel;

    private String currentSessionId;
    private String lastUserMessage;
    private String lastAiResponse; // last AI reply, fed to CreatePersonaSim so the simulated persona can react to it
    private int turnCounter;
    private int exchangeNumber; // 1 per AI reply (1st reply, 2nd reply, ...), independent of turnCounter's raw User+AI count

    public ConvoEngine() {
        this(new ClaudeConnector(), "claude-sonnet-4-6");
    }

    public ConvoEngine(LLMProvider llmProvider, String modelName) {
        this.llmProvider = llmProvider;
        this.modelName = modelName;
        // judge and persona-simulator are both independent of the model under test - judge so it never grades
        // its own output, persona-sim because roleplaying a character doesn't need the model-under-test's
        // capability and shouldn't spend its API budget either. Groq (hosting Llama) rather than Gemini, since
        // Gemini's free-tier daily quota is exhausted across every model variant tried.
        if (modelName.toLowerCase().contains("llama") || modelName.toLowerCase().contains("groq")) {
            this.judgeProvider = new ClaudeConnector();
            this.judgeModel = "claude-sonnet-4-6";
            this.personaSimProvider = new ClaudeConnector();
            this.personaSimModel = "claude-sonnet-4-6";
        } else {
            this.judgeProvider = new GroqConnector();
            this.judgeModel = "llama-3.3-70b-versatile";
            this.personaSimProvider = new GroqConnector();
            this.personaSimModel = "llama-3.3-70b-versatile";
        }
    }

    public String startSession(Persona persona, Scenario scenario, String mode) {
        history = new ArrayList<>();
        currentSessionId = UUID.randomUUID().toString();
        lastUserMessage = null;
        lastAiResponse = null;
        turnCounter = 0;
        exchangeNumber = 0;

        String initialPrompt;
        if ("Baseline".equals(mode)) {
            // Baseline = control: no persona manipulation, just the neutral version of the same topic
            systemPrompt = "";
            initialPrompt = scenario.baselinePrompt;
        } else {
            systemPrompt = persona.systemPromptOverride; // every convo resets playing field, for instance the persona must consider every different session
            // status reflects this persona's actual documentation status (it's one of the two axes the bias
            // study varies), but skill_level is held constant across all personas - it isn't one of those axes
            String status = persona.vulnerabilityFactors.contains("undocumented") ? "undocumented" : "valid and up to date";
            initialPrompt = scenario.initialPromptTemplate
                .replace("{location}", persona.location)
                .replace("{home_country}", persona.origin)
                .replace("{status}", status)
                .replace("{skill_level}", "basic");
        }

        DatabaseManager.saveSession(currentSessionId, persona.id, scenario.id, mode, Instant.now().toString());

        history.add("User: " + initialPrompt); // handles message sending, adding to AI history. uses the configured llmProvider
        DatabaseManager.saveTurn(currentSessionId, ++turnCounter, "User", initialPrompt, Instant.now().toString());
        lastUserMessage = initialPrompt;
        try {
            String response = llmProvider.sendMessage(systemPrompt, initialPrompt, modelName);
            history.add("AI: " + response);
            DatabaseManager.saveTurn(currentSessionId, ++turnCounter, "AI", response, Instant.now().toString());
            lastAiResponse = response;
            scoreAndSaveTurn(initialPrompt, response);
            return response;
        } catch (Exception e) {
            String err = "Error: " + e.getMessage();
            history.add("AI: " + err);
            lastAiResponse = err;
            return err;
        }
    }

    public void runAutomatedSession(Persona persona, Scenario scenario, int totalTurns, String mode) {
        startSession(persona, scenario, mode);
        for (int i = 1; i < totalTurns; i++) {
            String simulatedUserMessage = CreatePersonaSim.generateResponse(personaSimProvider, personaSimModel, persona, lastAiResponse, history);
            sendUserMessage(simulatedUserMessage);
        }
        // also save the final turn's score into the original session-level `scores` table (session_id primary
        // key, INSERT OR REPLACE) so all the existing reporting (heatmap, mean-by-persona/scenario) keeps working
        // unchanged - the full per-turn trajectory lives separately in turn_scores
        ExploitationScore score = Rules.evaluate(lastUserMessage, lastAiResponse, judgeProvider, judgeModel);
        DatabaseManager.saveScore(currentSessionId, score);
    }

    public String sendUserMessage(String userMessage) {
        history.add("User: " + userMessage);
        DatabaseManager.saveTurn(currentSessionId, ++turnCounter, "User", userMessage, Instant.now().toString());
        lastUserMessage = userMessage;

        try {
            String response = llmProvider.sendMessage(systemPrompt, userMessage, modelName);
            history.add("AI: " + response);
            DatabaseManager.saveTurn(currentSessionId, ++turnCounter, "AI", response, Instant.now().toString());
            lastAiResponse = response;
            scoreAndSaveTurn(userMessage, response);
            return response;
        } catch (Exception e) {
            String err = "Error: " + e.getMessage();
            history.add("AI: " + err);
            lastAiResponse = err;
            return err;
        }
    }

    // judge calls go to Groq (free tier), not the model under test, so scoring every turn costs zero extra
    // Claude credits - only the model-under-test's own reply calls do that, and those are fixed by turn count
    private void scoreAndSaveTurn(String userMessage, String aiResponse) {
        ExploitationScore score = Rules.evaluate(userMessage, aiResponse, judgeProvider, judgeModel);
        DatabaseManager.saveTurnScore(currentSessionId, ++exchangeNumber, score);
    }
}
