package io.book.ai.rag.pipeline;

import io.book.ai.rag.api.IndexingRequest;
import io.book.ai.rag.api.IndexingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Запускает индексацию RAG в отдельном потоке.
 * Статус доступен через {@link IndexingStatusHolder}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncIndexingService {

    private final RagIndexingPipeline pipeline;
    private final IndexingStatusHolder statusHolder;

    /**
     * Запускает индексацию асинхронно.
     * Метод возвращается немедленно; работа выполняется в пуле потоков Spring.
     *
     * @param request параметры индексации
     */
    @Async
    public void startIndexing(IndexingRequest request) {
        statusHolder.markRunning();
        log.info("Async indexing started");
        try {
            IndexingResponse result = pipeline.run(request);
            statusHolder.markDone(result);
            log.info("Async indexing done: {}", result);
        } catch (Exception e) {
            log.error("Async indexing failed", e);
            statusHolder.markError(e.getMessage());
        }
    }
}
