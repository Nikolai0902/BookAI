package io.book.ai.rag.api;

/**
 * Сравнение трёх режимов RAG для одного вопроса.
 *
 * @param question          исходный вопрос
 * @param raw               поиск без фильтра и без rewrite (базовый режим)
 * @param filtered          поиск с фильтром по порогу, без rewrite
 * @param rewrittenFiltered поиск с rewrite запроса и фильтром по порогу
 */
public record RagModesResponse(
        String question,
        RagQueryResponse raw,
        RagQueryResponse filtered,
        RagQueryResponse rewrittenFiltered
) {}
