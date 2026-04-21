package io.book.ai.handler.agent;

import io.book.ai.api.AgentChatRequest;
import io.book.ai.api.AgentChatResponse;
import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.llm.LlmResult;
import io.book.ai.repository.entity.AgentMessageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;
import java.util.UUID;

/**
 * Основной обработчик диалогового агента.
 * <p>
 * На каждый запрос сохраняет сообщение пользователя, формирует контекст
 * (полная история или сжатая через {@link AgentContextCompressor}),
 * вызывает LLM и возвращает ответ со статистикой токенов.
 */
@Component
@RequiredArgsConstructor
public class AgentBook {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String SUMMARY_SYSTEM_PREFIX = "## Conversation History Summary\n";

    private final AnthropicClient anthropicClient;
    private final AgentSessionStore sessionStore;
    private final AgentContextCompressor agentContextCompressor;

    @Value("${anthropic.model}")
    private final String defaultModel;

    @Value("${anthropic.max-tokens}")
    private final int maxTokens;

    /**
     * Обрабатывает входящее сообщение пользователя в рамках сессии.
     * Сохраняет сообщение, формирует контекст, вызывает LLM и возвращает ответ.
     *
     * @param request запрос с текстом, sessionId, моделью и флагом сжатия контекста
     * @return ответ агента с текстом, токенами и статистикой сессии
     */
    public AgentChatResponse chat(AgentChatRequest request) {
        String sessionId = StringUtils.hasText(request.sessionId()) ? request.sessionId() : UUID.randomUUID().toString();
        String model = StringUtils.hasText(request.model()) ? request.model() : defaultModel;
        boolean useCompression = request.useCompression();

        sessionStore.saveMessage(sessionId, ROLE_USER, request.message());

        LlmResult result;
        int recentAsIs;
        int summarized;

        if (useCompression) {
            List<AgentMessageEntity> rawHistory = sessionStore.loadRawHistory(sessionId);
            AgentContextCompressor.CompressedContext compressed =
                    agentContextCompressor.compress(rawHistory, sessionStore.getSummary(sessionId), model);

            if (compressed.summaryUpdated()) {
                sessionStore.saveSummary(sessionId, compressed.summaryText(), compressed.summarizedCount());
            }

            String systemPrompt = compressed.summaryText() != null
                    ? SUMMARY_SYSTEM_PREFIX + compressed.summaryText()
                    : null;

            result = callLlm(model, compressed.recentMessages(), sessionId, systemPrompt);
            recentAsIs = compressed.recentMessages().size();
            summarized = compressed.summarizedCount();
        } else {
            result = callLlm(model, sessionStore.loadHistory(sessionId), sessionId, null);
            recentAsIs = 0;
            summarized = 0;
        }

        sessionStore.saveAssistantMessage(sessionId, result.text(), result.inputTokens(), result.outputTokens());

        return buildResponse(sessionId, result, useCompression, recentAsIs, summarized);
    }

    private LlmResult callLlm(String model, List<Message> history, String sessionId, String systemPrompt) {
        try {
            return anthropicClient.callApi(new AnthropicRequest(model, maxTokens, systemPrompt, null, null, history));
        } catch (HttpClientErrorException e) {
            sessionStore.saveMessage(sessionId, ROLE_ASSISTANT, "[ERROR] " + e.getResponseBodyAsString());
            throw e;
        }
    }

    private AgentChatResponse buildResponse(String sessionId, LlmResult result,
                                             boolean compressionEnabled, int recentAsIs, int summarized) {
        return new AgentChatResponse(
                sessionId,
                result.text(),
                result.inputTokens(),
                result.outputTokens(),
                result.responseTimeMs(),
                sessionStore.getTotalInputTokens(sessionId),
                sessionStore.getTotalOutputTokens(sessionId),
                sessionStore.getMessageCount(sessionId) / 2,
                compressionEnabled,
                recentAsIs,
                summarized
        );
    }
}
