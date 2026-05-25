package io.book.ai.rag.api;

/**
 * Результат выполнения пайплайна индексации.
 *
 * @param fixedSizeIndexPath путь к сохранённому индексу FIXED_SIZE
 * @param structureIndexPath путь к сохранённому индексу STRUCTURE
 * @param stats              статистика сравнения стратегий чанкинга
 * @param elapsedMs          время выполнения пайплайна в миллисекундах
 */
public record IndexingResponse(
        String fixedSizeIndexPath,
        String structureIndexPath,
        ComparisonStats stats,
        long elapsedMs
) {}
