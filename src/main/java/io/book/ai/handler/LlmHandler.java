package io.book.ai.handler;

import io.book.ai.api.LlmAskResponse;
import io.book.ai.dto.AnthropicRequest;
import io.book.ai.dto.AnthropicResponse;
import io.book.ai.dto.Content;
import io.book.ai.dto.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class LlmHandler {

    private static final String BASE_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final String model;

    public LlmHandler(
            @Value("${anthropic.api-key}") String apiKey,
            @Value("${anthropic.model}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();
    }

    public LlmAskResponse ask(String prompt) {
        var request = new AnthropicRequest(model, 300, List.of(new Message("user", prompt)));

        var response = restClient.post()
                .body(request)
                .retrieve()
                .body(AnthropicResponse.class);

        String text = response.content().stream()
                .filter(c -> "text".equals(c.type()))
                .findFirst()
                .map(Content::text)
                .orElse("");

        return new LlmAskResponse(text);
    }
}
