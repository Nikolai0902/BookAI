package io.book.ai.handler;

import io.book.ai.api.LlmAskResponse;
import io.book.ai.api.LlmCompareResponse;
import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LlmHandler {

    private final AnthropicClient anthropicClient;

    @Value("${anthropic.model}")
    private final String model;

    public LlmAskResponse ask(String prompt, Double temperature) {
        var request = new AnthropicRequest(model, 600, null, null, temperature, List.of(new Message("user", prompt)));
        var result = anthropicClient.callApi(request);
        return new LlmAskResponse(result.text(), result.inputTokens(), result.outputTokens());
    }

    public LlmCompareResponse compare(String prompt, Double temperature) {
        var freeRequest = new AnthropicRequest(model, 300, null, null, temperature, List.of(new Message("user", prompt)));

        String systemPrompt = """
                Отвечай строго в формате JSON. Используй такую структуру:
                {"items": [{"rank": 1, "name": "...", "description": "..."}]}
                Не добавляй ничего вне JSON объекта.
                """;
        var controlledRequest = new AnthropicRequest(
                model, 150, systemPrompt, List.of("СТОП"), temperature, List.of(new Message("user", prompt))
        );

        var free = anthropicClient.callApi(freeRequest);
        var controlled = anthropicClient.callApi(controlledRequest);
        return new LlmCompareResponse(
                free.text(), controlled.text(),
                free.inputTokens() + controlled.inputTokens(),
                free.outputTokens() + controlled.outputTokens()
        );
    }
}
