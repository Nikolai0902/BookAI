package io.book.ai.api;

import io.book.ai.handler.task.TaskPhase;

public record TaskStateSnapshot(
        TaskPhase phase,
        String currentStep,
        String expectedAction,
        boolean paused
) {}
