package io.book.ai.rag.eval;

import io.book.ai.ollama.OllamaProperties;
import io.book.ai.rag.api.EvalQuestion;
import io.book.ai.rag.api.EvalReport;
import io.book.ai.rag.api.EvalResult;
import io.book.ai.rag.api.RagQueryRequest;
import io.book.ai.rag.api.RagQueryResponse;
import io.book.ai.rag.config.RagProperties;
import io.book.ai.rag.query.RagQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Сервис для пакетного прогона 10 контрольных вопросов и оценки качества RAG-ответов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagEvalService {

    private static final Pattern INLINE_REF = Pattern.compile("\\[\\d+]");

    private final RagQueryService queryService;
    private final RagProperties props;
    private final OllamaProperties ollamaProperties;

    @Value("${anthropic.model}")
    private String defaultAnthropicModel;

    /**
     * Прогоняет все 10 контрольных вопросов через RAG и возвращает агрегированный отчёт.
     *
     * @param template параметры запроса (topK, minScore, model, useLocal и т.д.); вопрос игнорируется
     * @return отчёт с детальными результатами и сводной статистикой
     * @throws IOException если файл индекса недоступен
     */
    public EvalReport runEval(RagQueryRequest template) throws IOException {
        List<EvalQuestion> questions = EvalQuestion.defaultQuestions();
        List<EvalResult> results = new ArrayList<>(questions.size());
        boolean useLocal = Boolean.TRUE.equals(template.useLocal());
        boolean ollamaEmbeddings = "ollama".equalsIgnoreCase(props.getEmbeddingProvider());

        for (int i = 0; i < questions.size(); i++) {
            EvalQuestion q = questions.get(i);
            log.info("Eval [{}/{}] (useLocal={}): {}", i + 1, questions.size(), useLocal, q.question());

            RagQueryRequest req = new RagQueryRequest(
                    q.question(),
                    template.topK(),
                    template.minScore(),
                    template.rewriteQuery(),
                    template.strategy(),
                    template.model(),
                    template.useLocal()
            );

            RagQueryResponse resp = queryService.queryWithRag(req);
            results.add(toEvalResult(q, resp));

            // Voyage free tier rate limit: пропускаем паузу для локальных эмбеддингов
            if (i < questions.size() - 1 && !ollamaEmbeddings) {
                log.info("Eval: waiting {}ms before next question (Voyage rate limit)", props.getRequestDelayMs());
                sleep(props.getRequestDelayMs());
            }
        }

        int withSources = (int) results.stream().filter(EvalResult::hasSources).count();
        int withQuotes = (int) results.stream().filter(EvalResult::hasInlineQuotes).count();
        int confident = (int) results.stream().filter(EvalResult::confident).count();
        long totalElapsed = results.stream().mapToLong(EvalResult::elapsedMs).sum();
        int totalOutput = results.stream().mapToInt(EvalResult::outputTokens).sum();
        double avgElapsed = results.isEmpty() ? 0d : (double) totalElapsed / results.size();
        String provider = resolveProvider(template, useLocal);

        log.info("Eval done [{}]: sources={}/{} quotes={}/{} confident={}/{} avgMs={} totalOut={}",
                provider, withSources, results.size(), withQuotes, results.size(),
                confident, results.size(), Math.round(avgElapsed), totalOutput);

        return new EvalReport(results, results.size(), withSources, withQuotes, confident,
                totalElapsed, avgElapsed, totalOutput, provider);
    }

    private EvalResult toEvalResult(EvalQuestion q, RagQueryResponse resp) {
        boolean hasSources = resp.citations() != null && !resp.citations().isEmpty();
        boolean hasInlineQuotes = resp.answer() != null && INLINE_REF.matcher(resp.answer()).find();
        float maxScore = resp.citations() == null ? 0f : resp.citations().stream()
                .map(RagQueryResponse.Citation::score)
                .reduce(0f, Float::max);

        return new EvalResult(
                q.id(),
                q.question(),
                resp.answer(),
                hasSources,
                hasInlineQuotes,
                resp.confident(),
                maxScore,
                resp.filteredChunks(),
                resp.elapsedMs(),
                resp.outputTokens()
        );
    }

    private String resolveProvider(RagQueryRequest template, boolean useLocal) {
        if (useLocal) {
            String m = template.model() != null ? template.model() : ollamaProperties.getModel();
            return m + " (local)";
        }
        return template.model() != null ? template.model() : defaultAnthropicModel;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
