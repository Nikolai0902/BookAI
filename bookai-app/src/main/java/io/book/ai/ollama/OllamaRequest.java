package io.book.ai.ollama;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.Map;

/**
 * Тело запроса к Ollama API {@code POST /api/chat}.
 *
 * @param model    идентификатор модели
 * @param messages история диалога
 * @param stream   всегда {@code false} — используем не-стриминговый режим
 * @param options  параметры генерации (temperature, num_ctx, num_predict и т.д.); {@code null} — дефолты Ollama
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OllamaRequest(
        String model,
        List<Message> messages,
        boolean stream,
        Map<String, Object> options
) {

    /**
     * Конструктор для обратной совместимости — без явных options.
     */
    public OllamaRequest(String model, List<Message> messages, boolean stream) {
        this(model, messages, stream, null);
    }

    /**
     * Одно сообщение диалога.
     *
     * @param role    роль — {@code "user"}, {@code "assistant"} или {@code "system"}
     * @param content текст сообщения
     */
    public record Message(String role, String content) {}
}
