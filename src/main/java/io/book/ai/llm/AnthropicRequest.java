package io.book.ai.llm;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Request body sent to the Anthropic Messages API.
 *
 * @param model          the model identifier (e.g. {@code claude-sonnet-4-6})
 * @param max_tokens     maximum number of tokens to generate in the response
 * @param system         optional system prompt that sets the assistant's behavior
 * @param stop_sequences optional list of strings that stop generation when encountered
 * @param messages       the conversation turns to send to the model
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnthropicRequest(
        String model,
        int max_tokens,
        String system,
        List<String> stop_sequences,
        Double temperature,
        List<Message> messages
) {

    /**
     * A single conversation turn.
     *
     * @param role    speaker role — either {@code "user"} or {@code "assistant"}
     * @param content the text content of the turn
     */
    public record Message(String role, String content) {}
}
