package io.book.ai.rag.embedding;

import io.book.ai.rag.chunking.Chunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис батчевой генерации эмбеддингов для списка чанков.
 * Делегирует выбранному {@link EmbeddingProvider} (Voyage AI или Ollama).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingBatchService {

    private final EmbeddingProvider embeddingProvider;

    /**
     * Генерирует эмбеддинги для всех переданных чанков.
     *
     * @param chunks список чанков
     * @return вектора эмбеддингов {@code float[]} в том же порядке, что входной список
     */
    public List<float[]> embedChunks(List<Chunk> chunks) {
        if (chunks.isEmpty()) return List.of();
        List<String> texts = chunks.stream().map(Chunk::text).toList();
        return embeddingProvider.embed(texts);
    }
}
