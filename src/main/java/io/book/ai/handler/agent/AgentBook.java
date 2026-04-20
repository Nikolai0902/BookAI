package io.book.ai.handler.agent;

import io.book.ai.api.AgentChatRequest;
import io.book.ai.api.AgentChatResponse;
import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.llm.LlmResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Agent that conducts multi-turn book-related conversations via the Anthropic API.
 *
 * <p>Conversation history is persisted between restarts through {@link AgentSessionStore},
 * so the agent remembers previous messages even after a restart.
 */
@Component
@RequiredArgsConstructor
public class AgentBook {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final int MAX_TOKENS = 1000;

    private final AnthropicClient anthropicClient;
    private final AgentSessionStore sessionStore;

    @Value("${anthropic.model}")
    private final String defaultModel;

    /**
     * Processes a chat message within a session and returns the agent's reply.
     *
     * <p>If {@code request.sessionId()} is blank or {@code null}, a new session is created
     * and its ID is included in the response so the client can continue the conversation.
     *
     * @param request the incoming request with the user message, optional session ID,
     *                and optional model override
     * @return the agent's response including session ID, reply text, and token usage
     */
    public AgentChatResponse chat(AgentChatRequest request) {
        String sessionId = resolveSessionId(request);
        String model = resolveModel(request);

        List<Message> history = buildHistory(sessionId, request.message());
        LlmResult result = anthropicClient.callApi(
                new AnthropicRequest(model, MAX_TOKENS, null, null, null, history));

        sessionStore.saveMessage(sessionId, ROLE_ASSISTANT, result.text());

        return toResponse(sessionId, result);
    }

    private String resolveSessionId(AgentChatRequest request) {
        String id = request.sessionId();
        return (id != null && !id.isBlank()) ? id : UUID.randomUUID().toString();
    }

    private String resolveModel(AgentChatRequest request) {
        String model = request.model();
        return (model != null && !model.isBlank()) ? model : defaultModel;
    }

    /**
     * Loads persisted history, appends the new user message, and saves it.
     */
    private List<Message> buildHistory(String sessionId, String userMessage) {
        List<Message> history = new ArrayList<>(sessionStore.loadHistory(sessionId));
        history.add(new Message(ROLE_USER, userMessage));
        sessionStore.saveMessage(sessionId, ROLE_USER, userMessage);
        return history;
    }

    private AgentChatResponse toResponse(String sessionId, LlmResult result) {
        return new AgentChatResponse(
                sessionId,
                result.text(),
                result.inputTokens(),
                result.outputTokens(),
                result.responseTimeMs()
        );
    }
}
