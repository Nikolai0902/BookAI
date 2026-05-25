package io.book.ai.rag.api;

import io.book.ai.rag.chunking.ChunkingStrategyType;

/**
 * Сравнение двух стратегий чанкинга по статистическим показателям.
 *
 * @param fixedSize статистика для стратегии FIXED_SIZE
 * @param structure статистика для стратегии STRUCTURE
 */
public record ComparisonStats(StrategyStats fixedSize, StrategyStats structure) {

    /**
     * Статистика одной стратегии чанкинга.
     *
     * @param strategy       тип стратегии
     * @param totalDocuments количество проиндексированных документов
     * @param totalChunks    общее количество чанков
     * @param avgChunkLength средняя длина чанка в символах
     * @param minChunkLength минимальная длина чанка
     * @param maxChunkLength максимальная длина чанка
     * @param avgWordCount   среднее количество слов в чанке
     * @param minWordCount   минимальное количество слов
     * @param maxWordCount   максимальное количество слов
     */
    public record StrategyStats(
            ChunkingStrategyType strategy,
            int totalDocuments,
            int totalChunks,
            double avgChunkLength,
            int minChunkLength,
            int maxChunkLength,
            double avgWordCount,
            int minWordCount,
            int maxWordCount
    ) {}
}
