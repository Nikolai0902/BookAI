package io.book.ai.rag.embedding;

import java.util.List;

/**
 * Ответ Voyage AI API на запрос генерации эмбеддингов.
 *
 * @param data  список объектов с эмбеддингами (по одному на каждый входной текст)
 * @param usage статистика использования токенов
 */
public record VoyageResponse(List<EmbeddingItem> data, Usage usage) {

    /**
     * Один элемент эмбеддинга из ответа API.
     *
     * @param index     позиция текста в исходном запросе (0-based)
     * @param embedding вектор эмбеддинга
     */
    public record EmbeddingItem(int index, List<Double> embedding) {}

    /**
     * Статистика использования токенов.
     *
     * @param total_tokens суммарное количество токенов в запросе
     */
    public record Usage(int total_tokens) {}
}
