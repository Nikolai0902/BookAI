package io.book.ai.rag.api;

/**
 * Сравнение ответов с RAG и без RAG для одного вопроса.
 *
 * @param question   заданный вопрос
 * @param withRag    ответ с инжекцией релевантных чанков
 * @param withoutRag ответ без дополнительного контекста
 */
public record RagCompareResponse(
        String question,
        RagQueryResponse withRag,
        RagQueryResponse withoutRag
) {}
