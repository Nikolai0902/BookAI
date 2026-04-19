package io.book.ai.handler;

import io.book.ai.api.LlmCompareResponse;
import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.llm.LlmResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class LlmHandler {

    private final AnthropicClient anthropicClient;

    @Value("${anthropic.model}")
    private final String defaultModel;

    public LlmResult ask(String prompt, Double temperature, String model) {
        String m = model != null ? model : defaultModel;
        var request = new AnthropicRequest(m, 600, null, null, temperature, List.of(new Message("user", prompt)));
        return anthropicClient.callApi(request);
    }

    public LlmCompareResponse compare(String prompt, Double temperature, String model) {
        String m = model != null ? model : defaultModel;
        var freeRequest = new AnthropicRequest(m, 300, null, null, temperature, List.of(new Message("user", prompt)));

        String systemPrompt = """
                Отвечай строго в формате JSON. Используй такую структуру:
                {"items": [{"rank": 1, "name": "...", "description": "..."}]}
                Не добавляй ничего вне JSON объекта.
                """;
        var controlledRequest = new AnthropicRequest(
                m, 150, systemPrompt, List.of("СТОП"), temperature, List.of(new Message("user", prompt))
        );

        var free = anthropicClient.callApi(freeRequest);
        var controlled = anthropicClient.callApi(controlledRequest);
        return new LlmCompareResponse(
                free.text(), controlled.text(),
                free.inputTokens() + controlled.inputTokens(),
                free.outputTokens() + controlled.outputTokens(),
                free.responseTimeMs() + controlled.responseTimeMs()
        );
    }
}
