package io.book.ai.ollama;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP-клиент для взаимодействия с локальным сервером Ollama.
 * Проксирует запросы к {@code POST /api/chat}.
 */
@Component
@RequiredArgsConstructor
public class OllamaClient {

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
     * Отправляет запрос к Ollama {@code /api/chat} и возвращает ответ модели.
     *
     * @param request сформированный запрос с историей и именем модели
     * @return ответ Ollama с текстом и статистикой
     */
    public OllamaResponse chat(OllamaRequest request) {
        return restClient.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaResponse.class);
    }

    /**
     * Генерирует эмбеддинги для списка текстов через Ollama {@code /api/embed}.
     *
     * @param request запрос с именем модели и списком текстов
     * @return ответ с матрицей эмбеддингов
     */
    public OllamaEmbedResponse embed(OllamaEmbedRequest request) {
        return restClient.post()
                .uri("/api/embed")
                .body(request)
                .retrieve()
                .body(OllamaEmbedResponse.class);
    }

    /**
     * Запрашивает список доступных моделей — используется для проверки статуса Ollama.
     *
     * @return строка JSON с {@code models[]}, или бросает исключение если Ollama недоступна
     */
    public String getTags() {
        return restClient.get()
                .uri("/api/tags")
                .retrieve()
                .body(String.class);
    }
}
