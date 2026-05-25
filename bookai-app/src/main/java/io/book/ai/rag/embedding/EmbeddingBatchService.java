package io.book.ai.rag.embedding;

import io.book.ai.rag.chunking.Chunk;
import io.book.ai.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Сервис батчевой генерации эмбеддингов для списка чанков.
 * Разбивает чанки на батчи по {@code rag.batch-size} и вызывает {@link VoyageClient}.
 * Между батчами выдерживает паузу {@code rag.request-delay-ms} для соблюдения rate limit.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingBatchService {

    private final VoyageClient voyageClient;
    private final RagProperties props;

    /**
     * Генерирует эмбеддинги для всех переданных чанков батчами.
     *
     * @param chunks список чанков
     * @return вектора эмбеддингов {@code float[]} в том же порядке, что входной список
     */
    public List<float[]> embedChunks(List<Chunk> chunks) {
        if (chunks.isEmpty()) return List.of();

        List<float[]> result = new ArrayList<>(chunks.size());
        int batchSize = props.getBatchSize();
        String model = props.getVoyageModel();
        long delayMs = props.getRequestDelayMs();
        int totalBatches = (int) Math.ceil((double) chunks.size() / batchSize);

        for (int i = 0; i < chunks.size(); i += batchSize) {
            int batchNum = i / batchSize + 1;
            int end = Math.min(i + batchSize, chunks.size());
            log.info("Embedding batch {}/{} (chunks {}-{})", batchNum, totalBatches, i, end - 1);

            List<String> texts = chunks.subList(i, end).stream()
                    .map(Chunk::text)
                    .toList();

            VoyageResponse response = voyageClient.embed(new VoyageRequest(texts, model));

            List<float[]> batchEmbeddings = response.data().stream()
                    .sorted(Comparator.comparingInt(VoyageResponse.EmbeddingItem::index))
                    .map(item -> toFloatArray(item.embedding()))
                    .toList();

            result.addAll(batchEmbeddings);

            if (delayMs > 0 && end < chunks.size()) {
                log.info("Rate limit pause: {}ms before next batch", delayMs);
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        return result;
    }

    private float[] toFloatArray(List<Double> doubles) {
        float[] arr = new float[doubles.size()];
        for (int i = 0; i < doubles.size(); i++) {
            arr[i] = doubles.get(i).floatValue();
        }
        return arr;
    }
}
