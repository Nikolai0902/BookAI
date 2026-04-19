package io.book.ai.api;

public record AgentChatResponse(
        String sessionId,
        String reply,
        int inputTokens,
        int outputTokens,
        long responseTimeMs
) {}
