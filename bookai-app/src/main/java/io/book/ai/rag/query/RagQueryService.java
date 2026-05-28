package io.book.ai.rag.query;

import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.LlmResult;
import io.book.ai.rag.api.RagModesResponse;
import io.book.ai.rag.api.RagQueryRequest;
import io.book.ai.rag.api.RagQueryResponse;
import io.book.ai.rag.chunking.ChunkingStrategyType;
import io.book.ai.rag.config.RagProperties;
import io.book.ai.rag.filter.FilterResult;
import io.book.ai.rag.filter.RelevanceFilter;
import io.book.ai.rag.rewrite.QueryRewriter;
import io.book.ai.rag.search.IndexSearchService;
import io.book.ai.rag.search.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Оркестратор RAG-запроса: rewrite → поиск → фильтрация → промпт → LLM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagQueryService {

    private static final String RAG_SYSTEM_PROMPT = """
            Ты — эксперт по документам. Строго соблюдай правила:

            1. Отвечай ТОЛЬКО на основе предоставленных фрагментов — без домыслов.
            2. Каждый ключевой факт подтверждай дословной цитатой из фрагмента в формате: «...текст...» [N].
            3. В конце ответа обязательно перечисли использованные источники:
               [N] source | section
            4. Если ни один фрагмент не содержит ответа на вопрос — ответь ТОЛЬКО:
               "Недостаточно информации для ответа. Пожалуйста, уточните вопрос."
               Никакого другого текста в этом случае.""";

    private static final String NO_CONTEXT_ANSWER =
            "Недостаточно информации для ответа. Пожалуйста, уточните вопрос.";

    private final IndexSearchService searchService;
    private final RelevanceFilter relevanceFilter;
    private final QueryRewriter queryRewriter;
    private final AnthropicClient anthropicClient;
    private final RagProperties props;

    @Value("${anthropic.model}")
    private String defaultModel;

    /**
     * Выполняет запрос с RAG: опциональный rewrite → поиск → фильтрация → LLM.
     *
     * @param request параметры запроса
     * @return ответ с цитатами, статистикой фильтрации и токен-статистикой
     * @throws IOException если файл индекса недоступен
     */
    public RagQueryResponse queryWithRag(RagQueryRequest request) throws IOException {
        ChunkingStrategyType strategy = request.strategy() != null
                ? request.strategy() : ChunkingStrategyType.FIXED_SIZE;
        int topK = request.topK() != null ? request.topK() : props.getTopK();
        float minScore = request.minScore() != null ? request.minScore() : props.getMinScore();
        String model = request.model() != null ? request.model() : defaultModel;
        boolean rewrite = Boolean.TRUE.equals(request.rewriteQuery());

        String effectiveQuery = rewrite
                ? queryRewriter.rewrite(request.question()) : request.question();

        List<SearchResult> candidates = searchService.search(effectiveQuery, strategy, topK);
        log.info("Found {} candidates for query: {}", candidates.size(), effectiveQuery);

        return buildResponse(request.question(), candidates, minScore, model,
                rewrite ? effectiveQuery : null);
    }

    /**
     * Сравнивает три режима за минимальное количество API-вызовов:
     * 1 Voyage-вызов для оригинального запроса (shared для raw и filtered),
     * 1 Haiku-вызов для rewrite,
     * 1 Voyage-вызов для переформулированного запроса,
     * 3 LLM-вызова для получения ответов.
     *
     * @param request параметры запроса
     * @return три ответа рядом
     * @throws IOException если файл индекса недоступен
     */
    public RagModesResponse queryModes(RagQueryRequest request) throws IOException {
        ChunkingStrategyType strategy = request.strategy() != null
                ? request.strategy() : ChunkingStrategyType.FIXED_SIZE;
        int topK = request.topK() != null ? request.topK() : props.getTopK();
        float minScore = request.minScore() != null ? request.minScore() : props.getMinScore();
        String model = request.model() != null ? request.model() : defaultModel;

        // Voyage call #1: оригинальный запрос (shared для raw и filtered)
        List<SearchResult> originalCandidates = searchService.search(
                request.question(), strategy, topK);
        log.info("Modes: original search returned {} candidates", originalCandidates.size());

        // Haiku call: rewrite запроса (не занимает Voyage-квоту)
        String rewrittenQuery = queryRewriter.rewrite(request.question());

        // Пауза перед вторым Voyage-вызовом (free tier: 3 RPM)
        log.info("Modes: waiting {}ms before second Voyage call", props.getRequestDelayMs());
        sleep(props.getRequestDelayMs());

        // Voyage call #2: переформулированный запрос
        List<SearchResult> rewrittenCandidates = searchService.search(
                rewrittenQuery, strategy, topK);
        log.info("Modes: rewritten search returned {} candidates", rewrittenCandidates.size());

        // LLM call #1: raw (без фильтра)
        RagQueryResponse raw = buildResponse(request.question(), originalCandidates, 0f, model, null);
        sleep(1500);

        // LLM call #2: filtered (с фильтром, оригинальный запрос)
        RagQueryResponse filtered = buildResponse(request.question(), originalCandidates, minScore, model, null);
        sleep(1500);

        // LLM call #3: rewritten + filtered
        RagQueryResponse rewrittenFiltered = buildResponse(
                request.question(), rewrittenCandidates, minScore, model, rewrittenQuery);

        return new RagModesResponse(request.question(), raw, filtered, rewrittenFiltered);
    }

    /**
     * Выполняет запрос без RAG: только вопрос, без дополнительного контекста.
     *
     * @param request параметры запроса
     * @return ответ без цитат
     */
    public RagQueryResponse queryWithoutRag(RagQueryRequest request) {
        String model = request.model() != null ? request.model() : defaultModel;

        AnthropicRequest llmReq = new AnthropicRequest(
                model, props.getMaxTokens(), null,
                null, null,
                List.of(new AnthropicRequest.Message("user", request.question()))
        );

        LlmResult llmResult = anthropicClient.callApi(llmReq);
        return new RagQueryResponse(
                llmResult.text(), List.of(),
                llmResult.inputTokens(), llmResult.outputTokens(),
                llmResult.responseTimeMs(),
                0, 0, null, true
        );
    }

    private RagQueryResponse buildResponse(String question, List<SearchResult> candidates,
                                           float minScore, String model, String rewrittenQuery) {
        FilterResult filtered = relevanceFilter.filter(candidates, minScore);
        List<SearchResult> results = filtered.results();
        log.info("buildResponse: filter={} kept={} removed={}", minScore, results.size(), filtered.removedCount());

        if (results.isEmpty()) {
            log.info("buildResponse: no chunks after filtering — returning no-context response");
            return noContextResponse(candidates.size(), rewrittenQuery);
        }

        String contextBlock = buildContextBlock(results);
        String userMessage = contextBlock + "\n=== ВОПРОС ===\n" + question;

        AnthropicRequest llmReq = new AnthropicRequest(
                model, props.getMaxTokens(), RAG_SYSTEM_PROMPT,
                null, null,
                List.of(new AnthropicRequest.Message("user", userMessage))
        );

        LlmResult llmResult = anthropicClient.callApi(llmReq);
        return new RagQueryResponse(
                llmResult.text(),
                buildCitations(results),
                llmResult.inputTokens(),
                llmResult.outputTokens(),
                llmResult.responseTimeMs(),
                candidates.size(),
                results.size(),
                rewrittenQuery,
                true
        );
    }

    private RagQueryResponse noContextResponse(int retrievedChunks, String rewrittenQuery) {
        return new RagQueryResponse(
                NO_CONTEXT_ANSWER,
                List.of(),
                0, 0, 0,
                retrievedChunks, 0,
                rewrittenQuery,
                false
        );
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildContextBlock(List<SearchResult> results) {
        if (results.isEmpty()) {
            return "=== КОНТЕКСТ ===\nРелевантные фрагменты не найдены.\n";
        }
        StringBuilder sb = new StringBuilder("=== КОНТЕКСТ ===\n");
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            String snippet = r.chunk().text().length() > 300
                    ? r.chunk().text().substring(0, 300) + "..." : r.chunk().text();
            sb.append(String.format("[%d] Источник: %s | Раздел: %s%n\"%s\"%n%n",
                    i + 1, r.chunk().source(), r.chunk().section(), snippet));
        }
        return sb.toString();
    }

    private List<RagQueryResponse.Citation> buildCitations(List<SearchResult> results) {
        return results.stream()
                .map(r -> {
                    int rank = results.indexOf(r) + 1;
                    String snippet = r.chunk().text().length() > 200
                            ? r.chunk().text().substring(0, 200) + "..." : r.chunk().text();
                    return new RagQueryResponse.Citation(
                            rank, r.chunk().source(), r.chunk().section(), r.score(), snippet);
                })
                .toList();
    }
}
