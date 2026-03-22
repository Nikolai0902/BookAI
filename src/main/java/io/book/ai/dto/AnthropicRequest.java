package io.book.ai.dto;

import java.util.List;

public record AnthropicRequest(String model, int max_tokens, List<Message> messages) {}
