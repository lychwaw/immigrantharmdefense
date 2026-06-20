package workingengine;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;

public class LLMconnect {
    private static final AnthropicClient client = AnthropicOkHttpClient.fromEnv();

    public static String sendMessage(String systemPrompt, String userMessage, String model) {
        MessageCreateParams params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(200L)
            .system(systemPrompt)
            .addUserMessage(userMessage)
            .build();

        Message response = client.messages().create(params);
        StringBuilder text = new StringBuilder();
        response.content().forEach(block -> block.text().ifPresent(t -> text.append(t.text())));
        return text.toString();
    }
}
