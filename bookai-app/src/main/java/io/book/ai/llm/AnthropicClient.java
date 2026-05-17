package io.book.ai.llm;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class AnthropicClient {

    private static final String BASE_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @Value("${anthropic.api-key}")
    private final String apiKey;

    private RestClient restClient;

    @PostConstruct
    private void init() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();
    }

    public LlmResult callApi(AnthropicRequest request) {
        long start = System.currentTimeMillis();
        var response = restClient.post()
                .body(request)
                .retrieve()
                .body(AnthropicResponse.class);
        long elapsed = System.currentTimeMillis() - start;

        String text = response.content().stream()
                .filter(c -> "text".equals(c.type()))
                .findFirst()
                .map(AnthropicResponse.Content::text)
                .orElse("");

        AnthropicResponse.Usage usage = response.usage();
        return new LlmResult(text, usage.input_tokens(), usage.output_tokens(), elapsed);
    }
}
