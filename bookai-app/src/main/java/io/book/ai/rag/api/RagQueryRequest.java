package io.book.ai.rag.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.book.ai.rag.chunking.ChunkingStrategyType;

/**
 * Запрос к RAG-эндпоинтам.
 *
 * @param question вопрос пользователя
 * @param topK     количество извлекаемых чанков (null → значение из конфига)
 * @param strategy стратегия индекса для поиска (null → FIXED_SIZE)
 * @param model    модель LLM (null → значение из конфига)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagQueryRequest(
        String question,
        Integer topK,
        ChunkingStrategyType strategy,
        String model
) {}
