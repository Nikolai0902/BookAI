package io.book.ai.controller;

import io.book.ai.api.BookRequest;
import io.book.ai.api.BookResponse;
import io.book.ai.handler.BookOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/book")
@RequiredArgsConstructor
public class BookController {

    private final BookOrchestrator orchestrator;

    @PostMapping
    public BookResponse ask(@Valid @RequestBody BookRequest request) {
        return orchestrator.handle(request);
    }
}
