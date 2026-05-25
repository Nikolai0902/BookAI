package io.book.ai.rag.embedding;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP-клиент для вызова Voyage AI Embeddings API.
 * Следует паттерну {@code AnthropicClient}: {@link RestClient} инициализируется
 * в {@code @PostConstruct} с заголовком авторизации.
 */
@Component
@RequiredArgsConstructor
public class VoyageClient {

    private static final String BASE_URL = "https://api.voyageai.com/v1/embeddings";

    @Value("${rag.voyage-api-key:}")
    private final String apiKey;

    private RestClient restClient;

    @PostConstruct
    private void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        restClient = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(BASE_URL)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * Отправляет батч текстов в Voyage AI и возвращает эмбеддинги.
     *
     * @param request запрос с текстами и именем модели
     * @return ответ с вектором эмбеддинга для каждого входного текста
     */
    public VoyageResponse embed(VoyageRequest request) {
        return restClient.post()
                .body(request)
                .retrieve()
                .body(VoyageResponse.class);
    }
}
