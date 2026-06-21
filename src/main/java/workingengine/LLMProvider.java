package workingengine;

import model.LLMResponse;

public interface LLMProvider {
    // scenarioId is null when simulating the persona's reply (no tools should be offered there) and the
    // active scenario's id when generating the AI-under-test's reply (lets the connector decide which mock tools to expose)
    LLMResponse sendMessage(String systemPrompt, String userMessage, String model, String scenarioId);
}
