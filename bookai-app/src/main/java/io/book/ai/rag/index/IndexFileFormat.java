package io.book.ai.rag.index;

import io.book.ai.rag.chunking.ChunkingStrategyType;

import java.util.List;

/**
 * Корневой объект JSON-индекса для одной стратегии чанкинга.
 *
 * @param strategy    стратегия, применённая при индексации
 * @param generatedAt метка времени генерации (ISO-8601)
 * @param totalChunks общее количество чанков в индексе
 * @param chunks      список записей с метаданными и эмбеддингами
 */
public record IndexFileFormat(
        ChunkingStrategyType strategy,
        String generatedAt,
        int totalChunks,
        List<IndexEntry> chunks
) {}
