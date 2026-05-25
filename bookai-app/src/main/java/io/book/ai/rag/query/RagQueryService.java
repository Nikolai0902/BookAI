package io.book.ai.rag.query;

import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.LlmResult;
import io.book.ai.rag.api.RagQueryRequest;
import io.book.ai.rag.api.RagQueryResponse;
import io.book.ai.rag.chunking.ChunkingStrategyType;
import io.book.ai.rag.config.RagProperties;
import io.book.ai.rag.search.IndexSearchService;
import io.book.ai.rag.search.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Оркестратор RAG-запроса: поиск → построение промпта → вызов LLM.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagQueryService {

    private static final String RAG_SYSTEM_PROMPT = """
            Ты — эксперт по документам. Ниже приведены релевантные фрагменты из базы знаний.
            Используй ТОЛЬКО эти фрагменты для ответа на вопрос.
            Если фрагменты не содержат ответ — явно скажи об этом.
            В конце ответа перечисли номера использованных источников в формате [1], [2].""";

    private final IndexSearchService searchService;
    private final AnthropicClient anthropicClient;
    private final RagProperties props;

    @Value("${anthropic.model}")
    private String defaultModel;

    /**
     * Выполняет запрос с RAG: поиск релевантных чанков, инжекция в промпт, вызов LLM.
     *
     * @param request параметры запроса
     * @return ответ с цитатами и токен-статистикой
     * @throws IOException если файл индекса недоступен
     */
    public RagQueryResponse queryWithRag(RagQueryRequest request) throws IOException {
        ChunkingStrategyType strategy = request.strategy() != null
                ? request.strategy() : ChunkingStrategyType.FIXED_SIZE;
        int topK = request.topK() != null ? request.topK() : props.getTopK();
        String model = request.model() != null ? request.model() : defaultModel;

        List<SearchResult> results = searchService.search(request.question(), strategy, topK);
        log.info("Found {} chunks for query: {}", results.size(), request.question());

        String contextBlock = buildContextBlock(results);
        String userMessage = contextBlock + "\n=== ВОПРОС ===\n" + request.question();

        AnthropicRequest llmReq = new AnthropicRequest(
                model, props.getMaxTokens(), RAG_SYSTEM_PROMPT,
                null, null,
                List.of(new AnthropicRequest.Message("user", userMessage))
        );

        LlmResult llmResult = anthropicClient.callApi(llmReq);

        List<RagQueryResponse.Citation> citations = buildCitations(results);
        return new RagQueryResponse(
                llmResult.text(), citations,
                llmResult.inputTokens(), llmResult.outputTokens(),
                llmResult.responseTimeMs()
        );
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
                llmResult.responseTimeMs()
        );
    }

    private String buildContextBlock(List<SearchResult> results) {
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
