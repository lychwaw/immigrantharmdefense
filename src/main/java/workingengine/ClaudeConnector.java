package workingengine;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

public class ClaudeConnector implements LLMProvider {
    private static final AnthropicClient client = AnthropicOkHttpClient.fromEnv(); // picks up env API key for claude model

    @Override
    public String sendMessage(String systemPrompt, String userMessage, String model) {
        MessageCreateParams params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(200L)
            .system(systemPrompt)
            .addUserMessage(userMessage) // parameters for the message sent to model includes these items.
            .build();

        Message response = client.messages().create(params); // males object and sends, then claude provides a response
        StringBuilder text = new StringBuilder();
        response.content().forEach(block -> block.text().ifPresent(t -> text.append(t.text()))); // each block must be searched and appended to final text string
        return text.toString();
    }
}
