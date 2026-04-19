package io.book.ai.handler;

import io.book.ai.api.BookRequest;
import io.book.ai.api.BookRequest.Filter;
import io.book.ai.api.BookResponse;
import io.book.ai.api.LlmCompareResponse;
import io.book.ai.llm.LlmResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BookOrchestrator {

    private final LlmHandler llmHandler;
    private final ReasoningHandler reasoningHandler;

    @Value("${anthropic.model}")
    private final String defaultModel;

    public BookResponse handle(BookRequest request) {
        String model = request.model();
        Filter filter = request.filter();
        String resolved = model != null ? model : defaultModel;

        if (filter == null) {
            var r = llmHandler.ask(request.prompt(), request.temperature(), model);
            return toResponse(r.text(), r.inputTokens(), r.outputTokens(), r.responseTimeMs(), resolved);
        }

        return switch (filter.type()) {
            case COMPARE -> {
                var r = llmHandler.compare(request.prompt(), request.temperature(), model);
                yield toResponse(toJson(r), r.inputTokens(), r.outputTokens(), r.responseTimeMs(), resolved);
            }
            case REASON -> {
                var r = resolveReasoning(request.prompt(), filter, model);
                yield toResponse(r.text(), r.inputTokens(), r.outputTokens(), r.responseTimeMs(), resolved);
            }
        };
    }

    private LlmResult resolveReasoning(String prompt, Filter filter, String model) {
        if (filter.strategy() == null) {
            return reasoningHandler.direct(prompt, model);
        }
        return switch (filter.strategy()) {
            case DIRECT      -> reasoningHandler.direct(prompt, model);
            case STEP_BY_STEP -> reasoningHandler.stepByStep(prompt, model);
            case META_PROMPT  -> reasoningHandler.metaPrompt(prompt, model);
            case EXPERT_PANEL -> reasoningHandler.expertPanel(prompt, model);
        };
    }

    private BookResponse toResponse(String answer, int inputTokens, int outputTokens, long responseTimeMs, String model) {
        double cost = computeCost(model, inputTokens, outputTokens);
        return new BookResponse(answer, inputTokens, outputTokens, responseTimeMs, cost);
    }

    private static double computeCost(String model, int inputTokens, int outputTokens) {
        double inputPrice, outputPrice;
        if (model.contains("haiku")) {
            inputPrice = 0.80; outputPrice = 4.00;
        } else if (model.contains("opus")) {
            inputPrice = 15.00; outputPrice = 75.00;
        } else {
            inputPrice = 3.00; outputPrice = 15.00;
        }
        return (inputTokens * inputPrice + outputTokens * outputPrice) / 1_000_000.0;
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
