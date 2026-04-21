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

/**
 * Базовые операции с LLM: одиночный запрос и сравнение двух режимов генерации.
 * <p>
 * Метод {@code ask} — свободный ответ без ограничений формата.
 * Метод {@code compare} — две параллельных генерации: свободная и строго в JSON,
 * что позволяет сравнить результаты разных режимов.
 */
@Component
@RequiredArgsConstructor
public class LlmHandler {

    private final AnthropicClient anthropicClient;

    @Value("${anthropic.model}")
    private final String defaultModel;

    /**
     * Отправляет одиночный запрос к LLM и возвращает свободный текстовый ответ.
     *
     * @param prompt      текст запроса пользователя
     * @param temperature температура генерации (null — использовать дефолт модели)
     * @param model       идентификатор модели (null — использовать модель из конфига)
     * @return результат с текстом ответа и статистикой токенов
     */
    public LlmResult ask(String prompt, Double temperature, String model) {
        String m = model != null ? model : defaultModel;
        var request = new AnthropicRequest(m, 600, null, null, temperature, List.of(new Message("user", prompt)));
        return anthropicClient.callApi(request);
    }

    /**
     * Выполняет два параллельных запроса на один и тот же промпт:
     * свободная генерация и строго структурированный JSON-ответ.
     *
     * @param prompt      текст запроса пользователя
     * @param temperature температура генерации (null — использовать дефолт модели)
     * @param model       идентификатор модели (null — использовать модель из конфига)
     * @return пара ответов с суммарной статистикой токенов
     */
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
