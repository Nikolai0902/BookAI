package io.book.ai.rag.chat.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.book.ai.rag.chunking.ChunkingStrategyType;

/**
 * Запрос к RAG-чату.
 *
 * @param message      сообщение пользователя
 * @param sessionId    идентификатор сессии (null = новая сессия)
 * @param topK         количество кандидатов при поиске (null → из конфига)
 * @param minScore     порог фильтрации (null → из конфига)
 * @param rewriteQuery переформулировать запрос перед поиском (null → false)
 * @param strategy     стратегия индекса (null → FIXED_SIZE)
 * @param model        модель LLM (null → из конфига)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagChatRequest(
        String message,
        String sessionId,
        Integer topK,
        Float minScore,
        Boolean rewriteQuery,
        ChunkingStrategyType strategy,
        String model
) {}
