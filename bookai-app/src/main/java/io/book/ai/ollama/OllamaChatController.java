package io.book.ai.ollama;

import io.book.ai.ollama.api.OllamaChatRequest;
import io.book.ai.ollama.api.OllamaChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST-контроллер для чата с локальной LLM через Ollama.
 */
@RestController
@RequestMapping("/api/ollama-chat")
@RequiredArgsConstructor
public class OllamaChatController {

    @Value("${ollama.base-url}")
    private final String baseUrl;

    @Value("${ollama.model}")
    private final String model;

    private final OllamaChatService chatService;
    private final OllamaClient ollamaClient;

    /**
     * Выполняет один ход диалога с Ollama.
     *
     * @param request сообщение пользователя и история предыдущих ходов
     * @return ответ модели с текстом и статистикой
     */
    @PostMapping("/chat")
    public OllamaChatResponse chat(@RequestBody OllamaChatRequest request) {
        return chatService.chat(request);
    }

    /**
     * Проверяет доступность Ollama и возвращает информацию о сконфигурированной модели.
     *
     * @return {@code available: true} если Ollama отвечает, иначе {@code available: false}
     */
    @GetMapping("/status")
    public Map<String, Object> status() {
        try {
            ollamaClient.getTags();
            return Map.of("available", true, "model", model, "baseUrl", baseUrl);
        } catch (Exception e) {
            return Map.of("available", false, "model", model, "baseUrl", baseUrl);
        }
    }
}
