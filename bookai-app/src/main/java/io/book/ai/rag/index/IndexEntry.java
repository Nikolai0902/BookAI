package io.book.ai.rag.index;

/**
 * Запись индекса: метаданные чанка плюс вектор эмбеддинга.
 *
 * @param chunkId    уникальный идентификатор
 * @param source     путь к исходному файлу
 * @param title      название документа
 * @param section    заголовок раздела
 * @param chunkIndex порядковый номер в документе (0-based)
 * @param text       текстовое содержимое чанка
 * @param wordCount  количество слов
 * @param embedding  вектор эмбеддинга (512 измерений для {@code voyage-3-lite})
 */
public record IndexEntry(
        String chunkId,
        String source,
        String title,
        String section,
        int chunkIndex,
        String text,
        int wordCount,
        float[] embedding
) {}
