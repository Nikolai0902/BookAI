package io.book.ai.api;

import io.book.ai.handler.context.ContextStrategyType;

public record AgentChatResponse(
        String sessionId,
        String reply,
        int inputTokens,
        int outputTokens,
        long responseTimeMs,
        int totalInputTokens,
        int totalOutputTokens,
        long turnNumber,
        ContextStrategyType strategy,
        int recentMessagesCount,
        int summarizedMessagesCount,
        /** DB-идентификатор сохранённого ответа ассистента; используется как checkpointMessageId при ветвлении. */
        long lastMessageId,
        MemoryLayersSnapshot memoryLayersSnapshot,
        TaskStateSnapshot taskState
) {}
