package io.book.ai.ollama.api;

import java.util.Map;

/**
 * Тело ответа от {@code POST /api/ollama-chat/chat}.
 *
 * @param answer       текст ответа модели
 * @param model        идентификатор модели
 * @param elapsedMs    время генерации в миллисекундах
 * @param evalCount    количество сгенерированных токенов
 * @param appliedOptions параметры, реально применённые к этому запросу (дефолты + overrides из UI)
 */
public record OllamaChatResponse(
        String answer,
        String model,
        long elapsedMs,
        int evalCount,
        Map<String, Object> appliedOptions
) {}
