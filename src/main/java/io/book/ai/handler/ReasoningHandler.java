package io.book.ai.handler;

import io.book.ai.dto.AnthropicRequest;
import io.book.ai.dto.AnthropicResponse;
import io.book.ai.dto.Content;
import io.book.ai.dto.Message;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class ReasoningHandler {

    private static final String BASE_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final String MD_SYSTEM = "Форматируй ответ в Markdown: используй заголовки, списки, **жирный** для ключевых слов.";

    private final RestClient restClient;
    private final String model;

    public ReasoningHandler(
            @Value("${anthropic.api-key}") String apiKey,
            @Value("${anthropic.model}") String model) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    // 1. Прямой ответ без инструкций
    public String direct(String task) {
        var request = new AnthropicRequest(model, 300, MD_SYSTEM, null,
                List.of(new Message("user", task)));
        return callApi(request);
    }

    // 2. Пошаговое решение
    public String stepByStep(String task) {
        var request = new AnthropicRequest(model, 500, MD_SYSTEM, null,
                List.of(new Message("user", task + "\n\nРеши задачу пошагово, объясняя каждый шаг.")));
        return callApi(request);
    }

    // 3. Мета-промпт: сначала генерируем промпт, потом решаем
    public String metaPrompt(String task) {
        var metaRequest = new AnthropicRequest(model, 200, null, null,
                List.of(new Message("user", "Составь оптимальный промпт для решения следующей задачи: " + task)));
        String generatedPrompt = callApi(metaRequest);

        var answerRequest = new AnthropicRequest(model, 300, MD_SYSTEM, null,
                List.of(new Message("user", generatedPrompt)));
        return callApi(answerRequest);
    }

    // 4. Группа экспертов
    public String expertPanel(String task) {
        String system = MD_SYSTEM + """

                Ты — группа из трёх экспертов, которые решают задачу вместе:
                - Аналитик: разбирает условие, выявляет ключевые факты
                - Инженер: предлагает практическое решение с расчётами
                - Критик: указывает на слабые места и предлагает улучшения
                Каждый эксперт даёт свой ответ по очереди. В конце — итоговый вывод.
                """;
        var request = new AnthropicRequest(model, 600, system, null,
                List.of(new Message("user", task)));
        return callApi(request);
    }

    private String callApi(AnthropicRequest request) {
        var response = restClient.post()
                .body(request)
                .retrieve()
                .body(AnthropicResponse.class);

        return response.content().stream()
                .filter(c -> "text".equals(c.type()))
                .findFirst()
                .map(Content::text)
                .orElse("");
    }
}
