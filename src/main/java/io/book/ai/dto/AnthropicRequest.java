package io.book.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnthropicRequest(
        String model,
        int max_tokens,
        String system,
        List<String> stop_sequences,
        List<Message> messages
) {}
