package io.book.ai.ollama.api;

import java.util.List;

/**
 * Тело запроса к {@code POST /api/ollama-chat/chat}.
 *
 * @param message последнее сообщение пользователя
 * @param history предыдущие сообщения диалога (из фронтенда)
 */
public record OllamaChatRequest(
        String message,
        List<HistoryMessage> history
) {

    /**
     * Одно сообщение из истории диалога на стороне клиента.
     *
     * @param role    роль — {@code "user"} или {@code "assistant"}
     * @param content текст сообщения
     */
    public record HistoryMessage(String role, String content) {}
}
