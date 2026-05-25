package io.book.ai.rag.controller;

import io.book.ai.rag.api.ComparisonStats;
import io.book.ai.rag.api.IndexingRequest;
import io.book.ai.rag.api.IndexingResponse;
import io.book.ai.rag.index.IndexStatsService;
import io.book.ai.rag.pipeline.RagIndexingPipeline;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * REST-контроллер для управления RAG-индексацией документов.
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagIndexingPipeline pipeline;
    private final IndexStatsService statsService;

    /**
     * Запускает полный пайплайн индексации с обеими стратегиями чанкинга.
     * Операция синхронная и может занимать несколько минут.
     *
     * @param request необязательный запрос с переопределением путей к документам
     * @return результат со статистикой и путями к созданным файлам индексов
     * @throws IOException при ошибках работы с файловой системой
     */
    @PostMapping("/index")
    public IndexingResponse index(
            @RequestBody(required = false) IndexingRequest request) throws IOException {
        return pipeline.run(request);
    }

    /**
     * Возвращает статистику сравнения стратегий по уже созданным индексам.
     * Требует предварительного вызова {@code POST /api/rag/index}.
     *
     * @return сравнительная статистика двух стратегий
     * @throws IOException если файлы индексов не найдены
     */
    @GetMapping("/stats")
    public ComparisonStats stats() throws IOException {
        return statsService.load();
    }
}
