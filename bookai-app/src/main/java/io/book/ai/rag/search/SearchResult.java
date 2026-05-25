package io.book.ai.rag.search;

import io.book.ai.rag.index.IndexEntry;

/**
 * Результат поиска: чанк и его косинусное сходство с запросом.
 *
 * @param chunk чанк из индекса
 * @param score косинусное сходство (от 0 до 1)
 */
public record SearchResult(IndexEntry chunk, float score) {}
