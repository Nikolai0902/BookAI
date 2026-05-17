package io.book.ai.llm;

public record LlmResult(String text, int inputTokens, int outputTokens, long responseTimeMs) {

    public LlmResult add(LlmResult other) {
        return new LlmResult(other.text, inputTokens + other.inputTokens, outputTokens + other.outputTokens, responseTimeMs + other.responseTimeMs);
    }
}
