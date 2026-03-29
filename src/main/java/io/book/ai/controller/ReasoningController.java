package io.book.ai.controller;

import io.book.ai.api.LlmAskResponse;
import io.book.ai.api.ReasoningRequest;
import io.book.ai.handler.ReasoningHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reasoning")
@RequiredArgsConstructor
public class ReasoningController {

    private final ReasoningHandler reasoningHandler;

    // 1. Прямой ответ
    @PostMapping("/1")
    public LlmAskResponse direct(@Valid @RequestBody ReasoningRequest request) {
        return new LlmAskResponse(reasoningHandler.direct(request.task()));
    }

    // 2. Пошаговое решение
    @PostMapping("/2")
    public LlmAskResponse stepByStep(@Valid @RequestBody ReasoningRequest request) {
        return new LlmAskResponse(reasoningHandler.stepByStep(request.task()));
    }

    // 3. Мета-промпт
    @PostMapping("/3")
    public LlmAskResponse metaPrompt(@Valid @RequestBody ReasoningRequest request) {
        return new LlmAskResponse(reasoningHandler.metaPrompt(request.task()));
    }

    // 4. Группа экспертов
    @PostMapping("/4")
    public LlmAskResponse expertPanel(@Valid @RequestBody ReasoningRequest request) {
        return new LlmAskResponse(reasoningHandler.expertPanel(request.task()));
    }

}
