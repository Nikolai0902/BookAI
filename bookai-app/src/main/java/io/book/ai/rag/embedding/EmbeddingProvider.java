package io.book.ai.rag.embedding;

import java.util.List;

/**
 * Абстракция провайдера эмбеддингов.
 * Реализации: {@link VoyageEmbeddingProvider} (Voyage AI) и {@link OllamaEmbeddingProvider} (локально).
 */
public interface EmbeddingProvider {

    /**
     * Генерирует эмбеддинги для списка текстов.
     *
     * @param texts список текстов
     * @return вектора эмбеддингов в том же порядке
     */
    List<float[]> embed(List<String> texts);
}
