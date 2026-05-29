package io.book.ai.rag.embedding;

import io.book.ai.ollama.OllamaEmbeddingClient;
import io.book.ai.ollama.OllamaEmbedRequest;
import io.book.ai.ollama.OllamaEmbedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Провайдер эмбеддингов через локальный Ollama ({@code POST /api/embed}).
 * Активен когда {@code rag.embedding-provider=ollama}.
 * Без rate-limit пауз — локальный сервер не ограничен.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rag.embedding-provider", havingValue = "ollama")
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    @Value("${rag.ollama-embedding-model:nomic-embed-text}")
    private final String embeddingModel;

    private final OllamaEmbeddingClient ollamaClient;

    private static final int BATCH_SIZE = 10;

    /**
     * Отправляет тексты батчами по {@value BATCH_SIZE} штук.
     * Каждый батч занимает ~6 секунд — хорошо вписывается в таймаут клиента.
     *
     * @param texts список текстов
     * @return вектора эмбеддингов в том же порядке
     */
    @Override
    public List<float[]> embed(List<String> texts) {
        if (texts.isEmpty()) return List.of();

        List<float[]> result = new ArrayList<>(texts.size());
        int totalBatches = (int) Math.ceil((double) texts.size() / BATCH_SIZE);
        log.info("Ollama embedding {} texts, {} batches, model={}", texts.size(), totalBatches, embeddingModel);

        for (int i = 0; i < texts.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, texts.size());
            int batchNum = i / BATCH_SIZE + 1;
            log.info("Ollama embedding batch {}/{} (texts {}-{})", batchNum, totalBatches, i, end - 1);

            List<String> batch = texts.subList(i, end);
            OllamaEmbedResponse response = ollamaClient.embed(new OllamaEmbedRequest(embeddingModel, batch));
            response.embeddings().stream().map(this::toFloatArray).forEach(result::add);
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
