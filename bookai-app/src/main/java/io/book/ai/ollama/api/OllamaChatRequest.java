package io.book.ai.ollama.api;

import java.util.List;

/**
 * Тело запроса к {@code POST /api/ollama-chat/chat}.
 *
 * @param message      последнее сообщение пользователя
 * @param history      предыдущие сообщения диалога (из фронтенда)
 * @param model        переопределение модели для этого запроса (nullable — берётся из конфига)
 * @param systemPrompt системный промпт первым сообщением (nullable)
 * @param options      override параметров генерации (nullable — мёрджится поверх дефолтов)
 */
public record OllamaChatRequest(
        String message,
        List<HistoryMessage> history,
        String model,
        String systemPrompt,
        Options options
) {

    /**
     * Одно сообщение из истории диалога на стороне клиента.
     *
     * @param role    роль — {@code "user"} или {@code "assistant"}
     * @param content текст сообщения
     */
    public record HistoryMessage(String role, String content) {}

    /**
     * Параметры генерации, передаваемые из UI. Любое поле может быть {@code null} —
     * в этом случае действует значение из {@code application.yml}.
     */
    public record Options(
            Double temperature,
            Double topP,
            Integer topK,
            Integer numCtx,
            Integer numPredict,
            Double repeatPenalty
    ) {}
}
