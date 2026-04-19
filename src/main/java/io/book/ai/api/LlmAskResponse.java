package io.book.ai.api;

public record LlmAskResponse(String answer, int inputTokens, int outputTokens) {}
