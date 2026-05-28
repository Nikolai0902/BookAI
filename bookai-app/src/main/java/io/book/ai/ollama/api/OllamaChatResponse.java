package io.book.ai.ollama.api;

/**
 * Тело ответа от {@code POST /api/ollama-chat/chat}.
 *
 * @param answer    текст ответа модели
 * @param model     идентификатор модели
 * @param elapsedMs время генерации в миллисекундах
 * @param evalCount количество сгенерированных токенов
 */
public record OllamaChatResponse(
        String answer,
        String model,
        long elapsedMs,
        int evalCount
) {}
