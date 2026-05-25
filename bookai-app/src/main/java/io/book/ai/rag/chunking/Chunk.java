package io.book.ai.rag.chunking;

/**
 * Один чанк документа с полным набором метаданных.
 *
 * @param chunkId    уникальный идентификатор чанка (UUID)
 * @param source     путь к исходному файлу
 * @param title      название документа (имя файла без расширения)
 * @param section    заголовок раздела или метка {@code FIXED_N} для фиксированной стратегии
 * @param chunkIndex порядковый индекс чанка в рамках документа (0-based)
 * @param text       текстовое содержимое чанка
 * @param wordCount  количество слов в чанке
 */
public record Chunk(
        String chunkId,
        String source,
        String title,
        String section,
        int chunkIndex,
        String text,
        int wordCount
) {}
