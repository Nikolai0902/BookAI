package io.book.ai.ollama;

/**
 * Тело ответа от Ollama API {@code POST /api/chat} (не-стриминговый режим).
 *
 * @param model              идентификатор модели
 * @param message            сгенерированное сообщение ассистента
 * @param done               {@code true} когда генерация завершена
 * @param eval_count         количество сгенерированных токенов
 * @param prompt_eval_count  количество токенов в промпте
 * @param total_duration     общее время генерации в наносекундах
 */
public record OllamaResponse(
        String model,
        Message message,
        boolean done,
        int eval_count,
        int prompt_eval_count,
        long total_duration
) {

    /**
     * Сообщение от ассистента в ответе Ollama.
     *
     * @param role    всегда {@code "assistant"}
     * @param content текст ответа
     */
    public record Message(String role, String content) {}
}
