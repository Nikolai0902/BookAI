package io.book.ai.api;

import jakarta.validation.constraints.NotBlank;

public record ReasoningRequest(@NotBlank String task) {}
