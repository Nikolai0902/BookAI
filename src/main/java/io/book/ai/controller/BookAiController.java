package io.book.ai.controller;

import io.book.ai.api.LlmAskRequest;
import io.book.ai.api.LlmAskResponse;
import io.book.ai.handler.LlmHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
public class BookAiController {

    private final LlmHandler llmHandler;

    @PostMapping("/ask")
    public LlmAskResponse ask(@Valid @RequestBody LlmAskRequest request) {
        return llmHandler.ask(request.prompt());
    }

}
