package workingengine;

public interface LLMProvider {
    String sendMessage(String systemPrompt, String userMessage, String model);
}
