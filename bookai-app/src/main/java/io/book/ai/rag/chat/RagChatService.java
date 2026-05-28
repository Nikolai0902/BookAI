package io.book.ai.rag.chat;

import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.llm.LlmResult;
import io.book.ai.rag.api.RagQueryResponse.Citation;
import io.book.ai.rag.chat.api.RagChatRequest;
import io.book.ai.rag.chat.api.RagChatResponse;
import io.book.ai.rag.chunking.ChunkingStrategyType;
import io.book.ai.rag.config.RagProperties;
import io.book.ai.rag.filter.RelevanceFilter;
import io.book.ai.rag.rewrite.QueryRewriter;
import io.book.ai.rag.search.IndexSearchService;
import io.book.ai.rag.search.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Оркестратор RAG-чата: хранит историю диалога, при каждом ходу ищет контекст через RAG,
 * строит промпт с историей и памятью задачи, вызывает LLM, обновляет память.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatService {

    private static final int HISTORY_MESSAGES = 10;

    private static final String NO_CONTEXT_ANSWER =
            "Недостаточно информации для ответа. Пожалуйста, уточните вопрос.";

    private static final String BASE_SYSTEM_PROMPT = """
            Ты — ассистент по документации. Строго соблюдай правила:

            1. Отвечай ТОЛЬКО на основе предоставленных фрагментов — без домыслов.
            2. Учитывай историю диалога — не повторяй то, что уже обсудили.
            3. Каждый ключевой факт подтверждай дословной цитатой: «...текст...» [N].
            4. В конце ответа обязательно перечисли использованные источники:
               [N] source | section
            5. Если ни один фрагмент не содержит ответа — ответь ТОЛЬКО:
               "Недостаточно информации. Пожалуйста, уточните вопрос.\"""";

    private final IndexSearchService searchService;
    private final RelevanceFilter relevanceFilter;
    private final QueryRewriter queryRewriter;
    private final AnthropicClient anthropicClient;
    private final RagChatSessionStore sessionStore;
    private final RagChatMemoryExtractor memoryExtractor;
    private final RagProperties props;

    @Value("${anthropic.model}")
    private String defaultModel;

    /**
     * Выполняет один ход RAG-чата: поиск контекста → LLM с историей → обновление памяти.
     *
     * @param request запрос с сообщением и параметрами сессии
     * @return ответ с цитатами, памятью задачи и статистикой
     * @throws IOException если индекс недоступен
     */
    public RagChatResponse chat(RagChatRequest request) throws IOException {
        String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();
        ChunkingStrategyType strategy = request.strategy() != null ? request.strategy() : ChunkingStrategyType.FIXED_SIZE;
        int topK = request.topK() != null ? request.topK() : props.getTopK();
        float minScore = request.minScore() != null ? request.minScore() : props.getMinScore();
        String model = request.model() != null ? request.model() : defaultModel;
        boolean rewrite = Boolean.TRUE.equals(request.rewriteQuery());

        String question = request.message();
        log.info("RagChat [{}] turn, question: {}", sessionId, question);

        // Загрузить историю и память до обращения к LLM
        List<Message> history = sessionStore.loadRecentMessages(sessionId, HISTORY_MESSAGES);
        String currentFacts = sessionStore.loadContextFacts(sessionId);

        // RAG: поиск + фильтрация
        String searchQuery = rewrite ? queryRewriter.rewrite(question) : question;
        List<SearchResult> candidates = searchService.search(searchQuery, strategy, topK);
        log.info("RagChat: found {} candidates", candidates.size());

        List<SearchResult> ragResults = relevanceFilter.filter(candidates, minScore).results();
        log.info("RagChat: {} chunks after filter (minScore={})", ragResults.size(), minScore);

        // Нет релевантного контекста — ранний выход без LLM
        if (ragResults.isEmpty()) {
            sessionStore.saveUserMessage(sessionId, question);
            sessionStore.saveAssistantMessage(sessionId, NO_CONTEXT_ANSWER, List.of(), 0, 0);
            int turn = (int) (sessionStore.getMessageCount(sessionId) / 2);
            return new RagChatResponse(sessionId, NO_CONTEXT_ANSWER, List.of(),
                    0, 0, 0, candidates.size(), 0, false, turn, currentFacts);
        }

        // Собрать сообщения для LLM: история + текущий вопрос с RAG-контекстом
        List<Message> messages = buildMessages(history, question, ragResults);
        String systemPrompt = buildSystemPrompt(currentFacts);

        long start = System.currentTimeMillis();
        AnthropicRequest llmReq = new AnthropicRequest(model, props.getMaxTokens(), systemPrompt,
                null, null, messages);
        LlmResult llmResult = anthropicClient.callApi(llmReq);
        long elapsed = System.currentTimeMillis() - start;

        List<Citation> citations = buildCitations(ragResults);

        // Сохранить оба сообщения
        sessionStore.saveUserMessage(sessionId, question);
        sessionStore.saveAssistantMessage(sessionId, llmResult.text(), citations,
                llmResult.inputTokens(), llmResult.outputTokens());

        int turn = (int) (sessionStore.getMessageCount(sessionId) / 2);

        // Обновить память задачи через Haiku
        String newFacts = memoryExtractor.extract(question, llmResult.text(), currentFacts);
        if (newFacts != null && !newFacts.isBlank()) {
            sessionStore.saveContextFacts(sessionId, newFacts);
        }

        return new RagChatResponse(
                sessionId,
                llmResult.text(),
                citations,
                llmResult.inputTokens(),
                llmResult.outputTokens(),
                elapsed,
                candidates.size(),
                ragResults.size(),
                true,
                turn,
                newFacts != null && !newFacts.isBlank() ? newFacts : currentFacts
        );
    }

    /**
     * Удаляет все данные сессии.
     *
     * @param sessionId идентификатор сессии
     */
    public void deleteSession(String sessionId) {
        sessionStore.deleteSession(sessionId);
    }

    private List<Message> buildMessages(List<Message> history, String question, List<SearchResult> ragResults) {
        List<Message> messages = new ArrayList<>(history);
        String contextBlock = buildContextBlock(ragResults);
        messages.add(new Message("user", contextBlock + "\n=== ВОПРОС ===\n" + question));
        return messages;
    }

    private String buildSystemPrompt(String facts) {
        if (facts == null || facts.isBlank()) {
            return BASE_SYSTEM_PROMPT;
        }
        return BASE_SYSTEM_PROMPT + "\n\n## Память задачи\n" + facts;
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

    private List<Citation> buildCitations(List<SearchResult> results) {
        return results.stream()
                .map(r -> {
                    int rank = results.indexOf(r) + 1;
                    String snippet = r.chunk().text().length() > 200
                            ? r.chunk().text().substring(0, 200) + "..." : r.chunk().text();
                    return new Citation(rank, r.chunk().source(), r.chunk().section(), r.score(), snippet);
                })
                .toList();
    }
}
