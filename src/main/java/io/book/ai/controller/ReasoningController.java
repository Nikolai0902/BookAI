package io.book.ai.controller;

import io.book.ai.api.ReasoningRequest;
import io.book.ai.handler.ReasoningHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reasoning")
@RequiredArgsConstructor
public class ReasoningController {

    private final ReasoningHandler reasoningHandler;

    // 1. Прямой ответ
    @PostMapping(value = "/1", produces = MediaType.TEXT_PLAIN_VALUE)
    public String direct(@Valid @RequestBody ReasoningRequest request) {
        return reasoningHandler.direct(request.task());
    }

    // 2. Пошаговое решение
    @PostMapping(value = "/2", produces = MediaType.TEXT_PLAIN_VALUE)
    public String stepByStep(@Valid @RequestBody ReasoningRequest request) {
        return reasoningHandler.stepByStep(request.task());
    }

    // 3. Мета-промпт
    @PostMapping(value = "/3", produces = MediaType.TEXT_PLAIN_VALUE)
    public String metaPrompt(@Valid @RequestBody ReasoningRequest request) {
        return reasoningHandler.metaPrompt(request.task());
    }

    // 4. Группа экспертов
    @PostMapping(value = "/4", produces = MediaType.TEXT_PLAIN_VALUE)
    public String expertPanel(@Valid @RequestBody ReasoningRequest request) {
        return reasoningHandler.expertPanel(request.task());
    }

}
