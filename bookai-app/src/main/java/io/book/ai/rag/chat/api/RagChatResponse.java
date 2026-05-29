package io.book.ai.rag.chat.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.book.ai.rag.api.RagQueryResponse.Citation;

import java.util.List;

/**
 * Ответ RAG-чата: ответ LLM, цитаты, статистика и текущая память задачи.
 *
 * @param sessionId      идентификатор сессии
 * @param answer         текстовый ответ модели
 * @param citations      список источников с фрагментами
 * @param inputTokens    токены текущего хода (вход)
 * @param outputTokens   токены текущего хода (выход)
 * @param elapsedMs      время ответа в миллисекундах
 * @param retrievedChunks чанков найдено до фильтра
 * @param filteredChunks  чанков передано в промпт
 * @param confident      true — LLM ответил по чанкам; false — «не знаю»
 * @param turnNumber     номер хода в сессии (1-based)
 * @param contextFacts   текущая память задачи в формате key: value
 * @param provider       идентификатор модели-провайдера (например "qwen2.5:3b (local)")
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagChatResponse(
        String sessionId,
        String answer,
        List<Citation> citations,
        int inputTokens,
        int outputTokens,
        long elapsedMs,
        int retrievedChunks,
        int filteredChunks,
        boolean confident,
        int turnNumber,
        String contextFacts,
        String provider
) {}
