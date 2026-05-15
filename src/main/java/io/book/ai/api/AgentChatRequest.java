package io.book.ai.api;

import io.book.ai.handler.context.ContextStrategyType;

public record AgentChatRequest(String message, String sessionId, String model, ContextStrategyType strategy, Boolean memoryEnabled) {}
