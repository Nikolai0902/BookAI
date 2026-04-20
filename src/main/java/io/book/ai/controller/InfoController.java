package io.book.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/info")
@RequiredArgsConstructor
public class InfoController {

    @Value("${anthropic.model}")
    private final String model;

    @Value("${anthropic.max-tokens}")
    private final int maxTokens;

    @GetMapping
    public Map<String, Object> info() {
        return Map.of("model", model, "maxTokens", maxTokens);
    }
}
