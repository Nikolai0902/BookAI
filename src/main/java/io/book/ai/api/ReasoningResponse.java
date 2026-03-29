package io.book.ai.api;

public record ReasoningResponse(
        String directAnswer,
        String stepByStepAnswer,
        String metaPromptAnswer,
        String expertPanelAnswer
) {}
