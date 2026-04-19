package io.book.ai.handler;

import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.llm.LlmResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReasoningHandler {

    private static final String MD_SYSTEM = "Форматируй ответ в Markdown: используй заголовки, списки, **жирный** для ключевых слов.";

    private final AnthropicClient anthropicClient;

    @Value("${anthropic.model}")
    private final String model;

    public LlmResult direct(String task) {
        var request = new AnthropicRequest(model, 300, MD_SYSTEM, null,
                List.of(new Message("user", task)));
        return anthropicClient.callApi(request);
    }

    public LlmResult stepByStep(String task) {
        var request = new AnthropicRequest(model, 500, MD_SYSTEM, null,
                List.of(new Message("user", task + "\n\nРеши задачу пошагово, объясняя каждый шаг.")));
        return anthropicClient.callApi(request);
    }

    public LlmResult metaPrompt(String task) {
        var metaRequest = new AnthropicRequest(model, 200, null, null,
                List.of(new Message("user", "Составь оптимальный промпт для решения следующей задачи: " + task)));
        var meta = anthropicClient.callApi(metaRequest);

        var answerRequest = new AnthropicRequest(model, 300, MD_SYSTEM, null,
                List.of(new Message("user", meta.text())));
        return meta.add(anthropicClient.callApi(answerRequest));
    }

    public LlmResult expertPanel(String task) {
        String system = MD_SYSTEM + """

                Ты — группа из трёх экспертов, которые решают задачу вместе:
                - Аналитик: разбирает условие, выявляет ключевые факты
                - Инженер: предлагает практическое решение с расчётами
                - Критик: указывает на слабые места и предлагает улучшения
                Каждый эксперт даёт свой ответ по очереди. В конце — итоговый вывод.
                """;
        var request = new AnthropicRequest(model, 600, system, null,
                List.of(new Message("user", task)));
        return anthropicClient.callApi(request);
    }
}
