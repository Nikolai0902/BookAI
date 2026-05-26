package io.book.ai.rag.filter;

import io.book.ai.rag.search.SearchResult;

import java.util.List;

/**
 * Результат фильтрации: чанки прошедшие порог и статистика отсева.
 *
 * @param results      чанки с score ≥ threshold
 * @param removedCount количество отсечённых чанков
 * @param threshold    применённый порог
 */
public record FilterResult(List<SearchResult> results, int removedCount, float threshold) {}
