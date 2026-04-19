package io.book.ai.api;

import jakarta.validation.constraints.NotBlank;

public record BookRequest(@NotBlank String prompt, Double temperature, Filter filter) {

    public record Filter(FilterType type, ReasoningStrategy strategy) {}

    public enum FilterType {
        COMPARE, REASON
    }

    public enum ReasoningStrategy {
        DIRECT, STEP_BY_STEP, META_PROMPT, EXPERT_PANEL
    }

}
