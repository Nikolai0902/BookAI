package io.book.ai.rag.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.book.ai.rag.chunking.ChunkingStrategyType;

/**
 * Запрос к RAG-эндпоинтам.
 *
 * @param question     вопрос пользователя
 * @param topK         количество кандидатов при поиске (null → из конфига)
 * @param minScore     порог отсечения нерелевантных чанков (null → из конфига, 0 → без фильтра)
 * @param rewriteQuery переформулировать запрос через LLM перед поиском (null → false)
 * @param strategy     стратегия индекса для поиска (null → FIXED_SIZE)
 * @param model        модель LLM (null → из конфига; для useLocal=true перебивает ollama.model)
 * @param useLocal     true — генерация через локальную Ollama; false/null — через Anthropic
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagQueryRequest(
        String question,
        Integer topK,
        Float minScore,
        Boolean rewriteQuery,
        ChunkingStrategyType strategy,
        String model,
        Boolean useLocal
) {
    /** Конструктор без useLocal — для обратной совместимости. */
    public RagQueryRequest(String question, Integer topK, Float minScore, Boolean rewriteQuery,
                           ChunkingStrategyType strategy, String model) {
        this(question, topK, minScore, rewriteQuery, strategy, model, null);
    }
}
