package io.book.ai.handler.agent;

import io.book.ai.api.AgentChatRequest;
import io.book.ai.api.AgentChatResponse;
import io.book.ai.handler.context.ContextResult;
import io.book.ai.handler.context.ContextStrategyOrchestrator;
import io.book.ai.handler.context.ContextStrategyType;
import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.llm.LlmResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.UUID;

/**
 * Основной обработчик одного хода диалогового агента.
 * <p>
 * Оркестрирует полный цикл обработки сообщения:
 * <ol>
 *   <li>Сохраняет сообщение пользователя в базе.</li>
 *   <li>Делегирует формирование контекста нужной стратегии через {@link ContextStrategyOrchestrator}.</li>
 *   <li>Вызывает LLM с подготовленным контекстом.</li>
 *   <li>Сохраняет ответ ассистента с токен-статистикой.</li>
 *   <li>Запускает постобработку стратегии (например, обновление фактов).</li>
 *   <li>Возвращает ответ клиенту со статистикой сессии.</li>
 * </ol>
 * Если {@code sessionId} не передан, генерируется новый UUID — так начинается новая сессия.
 * Стратегия по умолчанию — {@link ContextStrategyType#FULL_HISTORY}.
 */
@Component
@RequiredArgsConstructor
public class AgentBook {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";

    private final AnthropicClient anthropicClient;
    private final AgentSessionStore sessionStore;
    private final ContextStrategyOrchestrator orchestrator;

    @Value("${anthropic.model}")
    private final String defaultModel;

    @Value("${anthropic.max-tokens}")
    private final int maxTokens;

    /**
     * Обрабатывает входящее сообщение пользователя в рамках сессии.
     *
     * @param request запрос с текстом, необязательными {@code sessionId} и {@code model},
     *                а также выбранной стратегией контекста
     * @return ответ агента с текстом, статистикой токенов текущего хода и накопленными
     *         итогами сессии; при стратегии {@code STICKY_FACTS} дополнительно содержит
     *         актуальный снимок ключевых фактов
     */
    public AgentChatResponse chat(AgentChatRequest request) {
        String sessionId = StringUtils.hasText(request.sessionId()) ? request.sessionId() : UUID.randomUUID().toString();
        String model = StringUtils.hasText(request.model()) ? request.model() : defaultModel;
        ContextStrategyType strategy = request.strategy() != null ? request.strategy() : ContextStrategyType.FULL_HISTORY;

        sessionStore.saveMessage(sessionId, ROLE_USER, request.message());

        ContextResult ctx = orchestrator.buildContext(strategy, sessionId, model);
        LlmResult result = callLlm(model, ctx.messages(), sessionId, ctx.systemPrompt());

        long lastMessageId = sessionStore.saveAssistantMessage(sessionId, result.text(), result.inputTokens(), result.outputTokens());
        String factsSnapshot = orchestrator.afterLlmResponse(strategy, sessionId, model);

        return buildResponse(sessionId, result, strategy, ctx, factsSnapshot, lastMessageId);
    }

    /**
     * Вызывает LLM с историей и системным промптом.
     * При ошибке API сохраняет сообщение об ошибке в базе от имени ассистента,
     * чтобы история диалога оставалась консистентной, и пробрасывает исключение.
     */
    private LlmResult callLlm(String model, List<Message> history, String sessionId, String systemPrompt) {
        try {
            return anthropicClient.callApi(new AnthropicRequest(model, maxTokens, systemPrompt, null, null, history));
        } catch (HttpClientErrorException e) {
            sessionStore.saveMessage(sessionId, ROLE_ASSISTANT, "[ERROR] " + e.getResponseBodyAsString());
            throw e;
        }
    }

    private AgentChatResponse buildResponse(String sessionId, LlmResult result,
                                             ContextStrategyType strategy, ContextResult ctx,
                                             String factsSnapshot, long lastMessageId) {
        return new AgentChatResponse(
                sessionId,
                result.text(),
                result.inputTokens(),
                result.outputTokens(),
                result.responseTimeMs(),
                sessionStore.getTotalInputTokens(sessionId),
                sessionStore.getTotalOutputTokens(sessionId),
                sessionStore.getMessageCount(sessionId) / 2,
                strategy,
                ctx.recentMessagesCount(),
                ctx.summarizedMessagesCount(),
                factsSnapshot,
                lastMessageId
        );
    }
}
