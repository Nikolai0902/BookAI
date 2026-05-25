package io.book.ai.rag.chunking;

import io.book.ai.rag.document.RawDocument;

import java.util.List;

/**
 * Стратегия разбиения документа на чанки для последующей векторизации.
 */
public interface ChunkingStrategy {

    /**
     * Возвращает тип данной стратегии.
     *
     * @return тип стратегии
     */
    ChunkingStrategyType type();

    /**
     * Разбивает документ на список чанков с метаданными.
     *
     * @param document загруженный документ
     * @return список чанков
     */
    List<Chunk> chunk(RawDocument document);
}
