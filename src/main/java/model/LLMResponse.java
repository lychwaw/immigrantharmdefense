package model;

import java.util.Collections;
import java.util.List;

public class LLMResponse { // wraps the model's reply text alongside any tool calls it made, so tool misuse can be scored without parsing strings
    public String text;
    public List<ToolCallResult> toolCalls;

    public LLMResponse(String text) {
        this(text, Collections.emptyList());
    }

    public LLMResponse(String text, List<ToolCallResult> toolCalls) {
        this.text = text;
        this.toolCalls = toolCalls;
    }
}
