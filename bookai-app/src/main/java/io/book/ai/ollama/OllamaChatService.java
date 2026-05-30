package io.book.ai.ollama;

import io.book.ai.ollama.api.OllamaChatRequest;
import io.book.ai.ollama.api.OllamaChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис чата с локальной LLM через Ollama.
 * Собирает историю диалога, мёрджит параметры (дефолты из конфига + overrides из запроса)
 * и делегирует запрос {@link OllamaClient}.
 */
@Service
@RequiredArgsConstructor
public class OllamaChatService {

    private final OllamaProperties ollamaProperties;
    private final OllamaClient ollamaClient;

    /**
     * Выполняет один ход диалога с Ollama.
     * История диалога передаётся клиентом с каждым запросом — состояние не хранится на сервере.
     *
     * @param request сообщение пользователя, история, optional override модели/параметров/system-промпта
     * @return ответ модели с текстом, статистикой и snapshot применённых параметров
     */
    public OllamaChatResponse chat(OllamaChatRequest request) {
        List<OllamaRequest.Message> messages = new ArrayList<>();

        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            messages.add(new OllamaRequest.Message("system", request.systemPrompt()));
        }
        if (request.history() != null) {
            for (OllamaChatRequest.HistoryMessage h : request.history()) {
                messages.add(new OllamaRequest.Message(h.role(), h.content()));
            }
        }
        messages.add(new OllamaRequest.Message("user", request.message()));

        String model = request.model() != null && !request.model().isBlank()
                ? request.model() : ollamaProperties.getModel();
        Map<String, Object> options = mergeOptions(request.options());

        int estimated = ContextSizeGuard.estimateTokens(messages.stream()
                .map(OllamaRequest.Message::content).toList());
        int numCtx = resolveNumCtx(options);
        ContextSizeGuard.check(estimated, numCtx);

        OllamaRequest ollamaReq = new OllamaRequest(model, messages, false,
                options.isEmpty() ? null : options);
        OllamaResponse ollamaResp = ollamaClient.chat(ollamaReq);

        return new OllamaChatResponse(
                ollamaResp.message().content(),
                ollamaResp.model(),
                ollamaResp.total_duration() / 1_000_000,
                ollamaResp.eval_count(),
                options
        );
    }

    private Map<String, Object> mergeOptions(OllamaChatRequest.Options override) {
        Map<String, Object> merged = new LinkedHashMap<>(ollamaProperties.toOptionsMap());
        if (override == null) {
            return merged;
        }
        putIfNotNull(merged, "temperature", override.temperature());
        putIfNotNull(merged, "top_p", override.topP());
        putIfNotNull(merged, "top_k", override.topK());
        putIfNotNull(merged, "num_ctx", override.numCtx());
        putIfNotNull(merged, "num_predict", override.numPredict());
        putIfNotNull(merged, "repeat_penalty", override.repeatPenalty());
        return merged;
    }

    private static void putIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    /**
     * Достаёт {@code num_ctx} из мёрджнутой карты опций; при отсутствии — дефолт 4096.
     */
    private static int resolveNumCtx(Map<String, Object> options) {
        Object v = options.get("num_ctx");
        if (v instanceof Number n) {
            return n.intValue();
        }
        return 4096;
    }
}
