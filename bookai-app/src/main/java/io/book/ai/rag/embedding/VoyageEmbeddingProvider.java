package io.book.ai.rag.embedding;

import io.book.ai.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Провайдер эмбеддингов через Voyage AI.
 * Активен когда {@code rag.embedding-provider=voyage} (значение по умолчанию).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.embedding-provider", havingValue = "voyage", matchIfMissing = true)
public class VoyageEmbeddingProvider implements EmbeddingProvider {

    private final VoyageClient voyageClient;
    private final RagProperties props;

    /**
     * Отправляет тексты батчами в Voyage AI с паузами между запросами.
     *
     * @param texts список текстов
     * @return вектора эмбеддингов в том же порядке
     */
    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts.isEmpty()) return List.of();

        List<float[]> result = new ArrayList<>(texts.size());
        int batchSize = props.getBatchSize();
        String model = props.getVoyageModel();
        long delayMs = props.getRequestDelayMs();
        int totalBatches = (int) Math.ceil((double) texts.size() / batchSize);

        for (int i = 0; i < texts.size(); i += batchSize) {
            int batchNum = i / batchSize + 1;
            int end = Math.min(i + batchSize, texts.size());
            log.info("Voyage embedding batch {}/{} (texts {}-{})", batchNum, totalBatches, i, end - 1);

            List<String> batch = texts.subList(i, end);
            VoyageResponse response = voyageClient.embed(new VoyageRequest(batch, model));

            List<float[]> batchEmbeddings = response.data().stream()
                    .sorted(Comparator.comparingInt(VoyageResponse.EmbeddingItem::index))
                    .map(item -> toFloatArray(item.embedding()))
                    .toList();

            result.addAll(batchEmbeddings);

            if (delayMs > 0 && end < texts.size()) {
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
