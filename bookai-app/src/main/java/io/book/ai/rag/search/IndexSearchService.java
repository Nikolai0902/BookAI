package io.book.ai.rag.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.book.ai.rag.chunking.ChunkingStrategyType;
import io.book.ai.rag.config.RagProperties;
import io.book.ai.rag.embedding.VoyageClient;
import io.book.ai.rag.embedding.VoyageRequest;
import io.book.ai.rag.embedding.VoyageResponse;
import io.book.ai.rag.index.IndexEntry;
import io.book.ai.rag.index.IndexFileFormat;
import io.book.ai.rag.index.IndexWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Сервис семантического поиска по JSON-индексу.
 * Загружает индекс с диска, векторизует запрос через Voyage AI
 * и возвращает top-K чанков по косинусному сходству.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexSearchService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final VoyageClient voyageClient;
    private final IndexWriter indexWriter;
    private final RagProperties props;

    /**
     * Выполняет поиск по заданному индексу и возвращает top-K результатов.
     *
     * @param query    текст запроса
     * @param strategy стратегия индекса для поиска
     * @param topK     количество результатов
     * @return отсортированный список результатов (наиболее релевантные первыми)
     * @throws IOException если файл индекса не найден или повреждён
     */
    public List<SearchResult> search(String query, ChunkingStrategyType strategy, int topK)
            throws IOException {
        Path indexPath = indexWriter.getIndexPath(strategy);
        log.info("Loading index from {}", indexPath);
        IndexFileFormat index = MAPPER.readValue(indexPath.toFile(), IndexFileFormat.class);

        float[] queryVec = embedQuery(query);

        return index.chunks().stream()
                .map(chunk -> new SearchResult(chunk, cosineSimilarity(queryVec, chunk.embedding())))
                .sorted(Comparator.comparingDouble(SearchResult::score).reversed())
                .limit(topK)
                .toList();
    }

    private float[] embedQuery(String query) {
        VoyageResponse resp = voyageClient.embed(
                new VoyageRequest(List.of(query), props.getVoyageModel()));
        List<Double> embedding = resp.data().get(0).embedding();
        float[] vec = new float[embedding.size()];
        for (int i = 0; i < embedding.size(); i++) {
            vec[i] = embedding.get(i).floatValue();
        }
        return vec;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0f;
        return dot / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
