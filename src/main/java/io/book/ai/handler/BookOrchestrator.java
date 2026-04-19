package io.book.ai.handler;

import io.book.ai.api.BookRequest;
import io.book.ai.api.BookRequest.Filter;
import io.book.ai.api.BookResponse;
import io.book.ai.api.LlmCompareResponse;
import io.book.ai.llm.LlmResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookOrchestrator {

    private final LlmHandler llmHandler;
    private final ReasoningHandler reasoningHandler;

    public BookResponse handle(BookRequest request) {
        Filter filter = request.filter();

        if (filter == null) {
            var r = llmHandler.ask(request.prompt(), request.temperature());
            return new BookResponse(r.answer(), r.inputTokens(), r.outputTokens());
        }

        return switch (filter.type()) {
            case COMPARE -> {
                var r = llmHandler.compare(request.prompt(), request.temperature());
                yield new BookResponse(toJson(r), r.inputTokens(), r.outputTokens());
            }
            case REASON -> {
                var r = resolveReasoning(request.prompt(), filter);
                yield new BookResponse(r.text(), r.inputTokens(), r.outputTokens());
            }
        };
    }

    private LlmResult resolveReasoning(String prompt, Filter filter) {
        if (filter.strategy() == null) {
            return reasoningHandler.direct(prompt);
        }
        return switch (filter.strategy()) {
            case DIRECT -> reasoningHandler.direct(prompt);
            case STEP_BY_STEP -> reasoningHandler.stepByStep(prompt);
            case META_PROMPT -> reasoningHandler.metaPrompt(prompt);
            case EXPERT_PANEL -> reasoningHandler.expertPanel(prompt);
        };
    }

    private static String toJson(LlmCompareResponse r) {
        return "{\"freeAnswer\":\"" + escapeJson(r.freeAnswer()) +
               "\",\"controlledAnswer\":\"" + escapeJson(r.controlledAnswer()) + "\"}";
    }

    private static String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
