package io.book.ai.ollama;

import io.book.ai.ollama.api.OllamaChatRequest;
import io.book.ai.ollama.api.OllamaChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Сервис чата с локальной LLM через Ollama.
 * Собирает историю диалога и делегирует запрос {@link OllamaClient}.
 */
@Service
@RequiredArgsConstructor
public class OllamaChatService {

    @Value("${ollama.model}")
    private final String model;

    private final OllamaClient ollamaClient;

    /**
     * Выполняет один ход диалога с Ollama.
     * История диалога передаётся клиентом с каждым запросом — состояние не хранится на сервере.
     *
     * @param request сообщение пользователя и история предыдущих ходов
     * @return ответ модели с текстом и статистикой
     */
    public OllamaChatResponse chat(OllamaChatRequest request) {
        List<OllamaRequest.Message> messages = new ArrayList<>();

        if (request.history() != null) {
            for (OllamaChatRequest.HistoryMessage h : request.history()) {
                messages.add(new OllamaRequest.Message(h.role(), h.content()));
            }
        }
        messages.add(new OllamaRequest.Message("user", request.message()));

        OllamaRequest ollamaReq = new OllamaRequest(model, messages, false);
        OllamaResponse ollamaResp = ollamaClient.chat(ollamaReq);

        return new OllamaChatResponse(
                ollamaResp.message().content(),
                ollamaResp.model(),
                ollamaResp.total_duration() / 1_000_000,
                ollamaResp.eval_count()
        );
    }
}
