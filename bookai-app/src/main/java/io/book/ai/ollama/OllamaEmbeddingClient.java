package io.book.ai.ollama;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP-клиент для Ollama {@code POST /api/embed}.
 * Использует увеличенный таймаут чтения (600 сек) — локальная CPU-генерация эмбеддингов
 * может занимать до нескольких минут на батч.
 */
@Component
@RequiredArgsConstructor
public class OllamaEmbeddingClient {

    @Value("${ollama.base-url}")
    private final String baseUrl;

    private RestClient restClient;

    @PostConstruct
    private void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(600_000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Генерирует эмбеддинги для списка текстов.
     *
     * @param request запрос с моделью и текстами
     * @return ответ с матрицей эмбеддингов
     */
    public OllamaEmbedResponse embed(OllamaEmbedRequest request) {
        return restClient.post()
                .uri("/api/embed")
                .body(request)
                .retrieve()
                .body(OllamaEmbedResponse.class);
    }
}
