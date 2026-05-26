package io.book.ai.llm;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(120_000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(BASE_URL)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("Connection", "close")
                .build();
    }

    public LlmResult callApi(AnthropicRequest request) {
        long start = System.currentTimeMillis();
        AnthropicResponse response = callRaw(request);
        long elapsed = System.currentTimeMillis() - start;

        String text = response.content().stream()
                .filter(c -> "text".equals(c.type()))
                .findFirst()
                .map(AnthropicResponse.Content::text)
                .orElse("");

        AnthropicResponse.Usage usage = response.usage();
        return new LlmResult(text, usage.input_tokens(), usage.output_tokens(), elapsed);
    }

    /**
     * Выполняет HTTP-запрос к Anthropic API и возвращает полный сырой ответ.
     * Используется агентом для обработки ответов с {@code tool_use}-блоками.
     *
     * @param request сформированный запрос к API
     * @return полный ответ, включая {@code stop_reason} и все контент-блоки
     */
    public AnthropicResponse callRaw(AnthropicRequest request) {
        return restClient.post()
                .body(request)
                .retrieve()
                .body(AnthropicResponse.class);
    }
}
