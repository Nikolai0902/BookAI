package io.book.ai.handler.agent;

import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Извлекает и обновляет рабочую память (блок ключевых фактов) из последнего обмена с помощью LLM.
 * Используется {@link AgentMemoryManager} для всех стратегий контекста.
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
                You maintain working memory for the current session as key: value facts.

                Capture facts in these groups (use these key names where applicable):
                - user_name, user_profession, user_language — who the user is
                - task — what the user is currently working on or asking about
                - goal — the explicit goal or desired outcome
                - constraints — limitations, requirements, non-negotiables
                - decisions — choices the user has made during this session
                - progress — what has been done or resolved so far

                Rules:
                - ALWAYS copy ALL existing facts unchanged unless the new exchange explicitly updates them.
                - Add new facts found in the new exchange.
                - Skip facts that have no value (nothing was mentioned).
                - Return ONLY key: value lines, one per line, no explanations, no headers.""",
                null, null,
                List.of(new Message("user", userContent))
        );
        return anthropicClient.callApi(req).text();
    }
}
