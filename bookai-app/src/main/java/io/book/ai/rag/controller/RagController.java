package io.book.ai.rag.controller;

import io.book.ai.rag.api.ComparisonStats;
import io.book.ai.rag.api.EvalQuestion;
import io.book.ai.rag.api.EvalReport;
import io.book.ai.rag.api.IndexingRequest;
import io.book.ai.rag.api.IndexingResponse;
import io.book.ai.rag.api.RagCompareResponse;
import io.book.ai.rag.api.RagModesResponse;
import io.book.ai.rag.api.RagQueryRequest;
import io.book.ai.rag.api.RagQueryResponse;
import io.book.ai.rag.eval.RagEvalService;
import io.book.ai.rag.index.IndexStatsService;
import io.book.ai.rag.pipeline.RagIndexingPipeline;
import io.book.ai.rag.query.RagQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * REST-контроллер для управления RAG-индексацией и запросами.
 */
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagIndexingPipeline pipeline;
    private final IndexStatsService statsService;
    private final RagQueryService queryService;
    private final RagEvalService evalService;

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

    /**
     * Выполняет запрос с RAG: находит релевантные чанки и инжектирует в промпт.
     *
     * @param request вопрос и параметры поиска
     * @return ответ LLM с цитатами и токен-статистикой
     * @throws IOException если файл индекса недоступен
     */
    @PostMapping("/query")
    public RagQueryResponse query(@RequestBody RagQueryRequest request) throws IOException {
        return queryService.queryWithRag(request);
    }

    /**
     * Сравнивает ответы с RAG и без RAG для одного вопроса.
     *
     * @param request вопрос и параметры поиска
     * @return оба ответа рядом для сравнения
     * @throws IOException если файл индекса недоступен
     */
    @PostMapping("/compare")
    public RagCompareResponse compare(@RequestBody RagQueryRequest request) throws IOException {
        RagQueryResponse withRag = queryService.queryWithRag(request);
        RagQueryResponse withoutRag = queryService.queryWithoutRag(request);
        return new RagCompareResponse(request.question(), withRag, withoutRag);
    }

    /**
     * Сравнивает три режима RAG для одного вопроса:
     * RAW (без фильтра), FILTERED (с порогом), REWRITTEN+FILTERED (rewrite + порог).
     *
     * @param request вопрос, topK и minScore для сравнения
     * @return три ответа рядом с метриками фильтрации
     * @throws IOException если файл индекса недоступен
     */
    @PostMapping("/modes")
    public RagModesResponse modes(@RequestBody RagQueryRequest request) throws IOException {
        return queryService.queryModes(request);
    }

    /**
     * Возвращает 10 контрольных вопросов для оценки качества RAG-системы.
     *
     * @return список вопросов с ожидаемыми ответами и источниками
     */
    @GetMapping("/eval/questions")
    public List<EvalQuestion> evalQuestions() {
        return EvalQuestion.defaultQuestions();
    }

    /**
     * Прогоняет все 10 контрольных вопросов через RAG и возвращает отчёт:
     * наличие источников, встроенных цитат и уверенности ответа для каждого вопроса.
     *
     * @param request опциональные параметры (topK, minScore, model); вопрос игнорируется
     * @return отчёт со сводной статистикой и детальными результатами
     * @throws IOException если файл индекса недоступен
     */
    @PostMapping("/eval/run")
    public EvalReport evalRun(
            @RequestBody(required = false) RagQueryRequest request) throws IOException {
        RagQueryRequest template = request != null ? request
                : new RagQueryRequest(null, null, null, null, null, null);
        return evalService.runEval(template);
    }
}
