package io.book.ai.api;

import jakarta.validation.constraints.NotBlank;

public record LlmAskRequest(@NotBlank String prompt) {}
