package io.book.ai.handler.context.strategy.sticky;

import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Извлекает и обновляет блок ключевых фактов из истории диалога с помощью LLM.
 * <p>
 * Всегда использует модель {@code claude-haiku} для снижения стоимости,
 * так как задача извлечения фактов не требует мощи старших моделей.
 * Используется стратегией {@link StickyFactsStrategy}.
 */
@Component
@RequiredArgsConstructor
public class FactsExtractor {

    private static final String HAIKU_MODEL = "claude-haiku-4-5-20251001";

    private final AnthropicClient anthropicClient;

    /**
     * Извлекает или обновляет список ключевых фактов диалога.
     * <p>
     * Если {@code existingFacts} не {@code null}, LLM дополняет существующий блок
     * на основе переданных сообщений — вызывающий код должен передавать только
     * новые сообщения (последний ход), а не всю историю.
     * Если фактов ещё нет, строит список с нуля по полной истории.
     * Ответ содержит только строки формата {@code key: value}.
     *
     * @param history       сообщения для обработки: при первом вызове — вся история,
     *                      при обновлении — только последние два сообщения текущего хода
     * @param existingFacts текущий блок фактов в формате «key: value», либо {@code null}
     * @return обновлённый блок фактов в виде строки «key: value» на каждой строке
     */
    public String extract(List<Message> history, String existingFacts) {
        String conv = history.stream()
                .map(m -> m.role() + ": " + m.content())
                .collect(Collectors.joining("\n\n"));

        String userContent = existingFacts != null
                ? "Existing facts:\n" + existingFacts + "\n\nUpdate with new conversation:\n" + conv
                : "Extract key facts from:\n" + conv;

        AnthropicRequest req = new AnthropicRequest(
                HAIKU_MODEL, 800,
                """
                You maintain a key-value fact list about an ongoing conversation.
                Rules:
                - ALWAYS copy ALL existing facts into your response, even if the new exchange does not mention them.
                - Only update a fact's value if the new exchange explicitly changes it.
                - Add new facts extracted from the new exchange.
                - Return ONLY key: value lines, one per line, no explanations.""",
                null, null,
                List.of(new Message("user", userContent))
        );
        return anthropicClient.callApi(req).text();
    }
}
