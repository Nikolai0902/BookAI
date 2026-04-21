package io.book.ai.api;

public record AgentChatRequest(String message, String sessionId, String model, boolean useCompression) {}
