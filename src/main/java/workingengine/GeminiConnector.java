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

public class GeminiConnector implements LLMProvider {
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";
    private static final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    @Override
    public String sendMessage(String systemPrompt, String userMessage, String model) {
        String apiKey = System.getenv("GEMINI_API_KEY");

        JsonObject userPart = new JsonObject();
        userPart.addProperty("text", userMessage);
        JsonArray userParts = new JsonArray();
        userParts.add(userPart); // gemini needs user message in parts

        JsonObject userContent = new JsonObject();
        userContent.addProperty("role", "user");
        userContent.add("parts", userParts);

        JsonArray contents = new JsonArray();
        contents.add(userContent);

        JsonObject body = new JsonObject();
        body.add("contents", contents);

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            JsonObject systemPart = new JsonObject();
            systemPart.addProperty("text", systemPrompt);
            JsonArray systemParts = new JsonArray();
            systemParts.add(systemPart);

            JsonObject systemInstruction = new JsonObject();
            systemInstruction.add("parts", systemParts);
            body.add("systemInstruction", systemInstruction);
        }

        String url = API_URL + model + ":generateContent?key=" + apiKey;

        Request request = new Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .post(RequestBody.create(gson.toJson(body), MediaType.parse("application/json")))
            .build();

        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            JsonObject json = gson.fromJson(responseBody, JsonObject.class);
            if (!response.isSuccessful() || json == null || !json.has("candidates")) {
                throw new RuntimeException("Gemini request failed (HTTP " + response.code() + "): " + responseBody);
            }
            return json.getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
