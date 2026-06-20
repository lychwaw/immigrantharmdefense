package workingengine;

import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OpenRouterConnector implements LLMProvider {
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();
    private static final int MAX_RETRIES = 2;

    @Override
    public String sendMessage(String systemPrompt, String userMessage, String model) {
        String apiKey = System.getenv("OPENROUTER_API_KEY");

        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);

        JsonObject userMsg = new JsonObject();
        userMsg.addProperty("role", "user");
        userMsg.addProperty("content", userMessage);

        JsonArray messages = new JsonArray();
        messages.add(systemMsg);
        messages.add(userMsg);

        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);

        Request request = new Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer " + apiKey)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
            .build();

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                JsonObject json = gson.fromJson(responseBody, JsonObject.class);

                if (response.code() == 429 && attempt < MAX_RETRIES) {
                    sleepBeforeRetry(response); // upstream free-tier rate limit - wait the suggested time and retry
                    continue;
                }

                if (!response.isSuccessful() || json == null || !json.has("choices")) {
                    throw new RuntimeException("OpenRouter request failed (HTTP " + response.code() + "): " + responseBody);
                }
                return json.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
            } catch (IOException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        throw new RuntimeException("OpenRouter request failed: still rate-limited after " + MAX_RETRIES + " retries");
    }

    private static void sleepBeforeRetry(Response response) {
        long waitSeconds = 5;
        String retryAfter = response.header("Retry-After");
        if (retryAfter != null) {
            try {
                waitSeconds = Long.parseLong(retryAfter.trim());
            } catch (NumberFormatException ignored) {
                // header wasn't a plain integer, fall back to default wait
            }
        }
        try {
            Thread.sleep(Math.min(waitSeconds, 30) * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
