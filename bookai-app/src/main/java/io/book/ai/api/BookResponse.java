package io.book.ai.api;

public record BookResponse(String answer, int inputTokens, int outputTokens, long responseTimeMs, double costUsd) {}
