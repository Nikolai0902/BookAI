package io.book.ai.handler.agent;

import io.book.ai.api.AgentChatRequest;
import io.book.ai.api.AgentChatResponse;
import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class BookAgent {

    private final AnthropicClient anthropicClient;

    @Value("${anthropic.model}")
    private final String defaultModel;

    private final Map<String, AgentSession> sessions = new ConcurrentHashMap<>();

    public AgentChatResponse chat(AgentChatRequest request) {
        String resolvedId = (request.sessionId() != null && !request.sessionId().isBlank())
                ? request.sessionId()
                : UUID.randomUUID().toString();

        AgentSession session = sessions.computeIfAbsent(resolvedId, AgentSession::new);
        session.addUserMessage(request.message());

        String m = (request.model() != null && !request.model().isBlank()) ? request.model() : defaultModel;
        var apiRequest = new AnthropicRequest(m, 1000, null, null, null, session.getHistory());
        var result = anthropicClient.callApi(apiRequest);

        session.addAssistantMessage(result.text());

        return new AgentChatResponse(
                resolvedId,
                result.text(),
                result.inputTokens(),
                result.outputTokens(),
                result.responseTimeMs()
        );
    }
}
