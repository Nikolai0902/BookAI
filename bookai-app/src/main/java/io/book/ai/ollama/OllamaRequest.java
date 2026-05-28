package io.book.ai.ollama;

import java.util.List;

/**
 * Тело запроса к Ollama API {@code POST /api/chat}.
 *
 * @param model    идентификатор модели
 * @param messages история диалога
 * @param stream   всегда {@code false} — используем не-стриминговый режим
 */
public record OllamaRequest(
        String model,
        List<Message> messages,
        boolean stream
) {

    /**
     * Одно сообщение диалога.
     *
     * @param role    роль — {@code "user"} или {@code "assistant"}
     * @param content текст сообщения
     */
    public record Message(String role, String content) {}
}
