package io.book.ai.rag.chat;

import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Извлекает и обновляет память задачи из последнего обмена в диалоге.
 * Использует Claude Haiku для анализа обмена и обновления ключевых фактов.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagChatMemoryExtractor {

    private static final String HAIKU_MODEL = "claude-haiku-4-5-20251001";

    private static final String SYSTEM_PROMPT =
            "Проанализируй последний обмен в диалоге и обнови память задачи.\n" +
            "Верни ТОЛЬКО строки в формате key: value (один факт — одна строка).\n" +
            "Допустимые ключи: goal, clarifications, constraints, terms\n" +
            "- goal: главная цель пользователя в этом диалоге\n" +
            "- clarifications: что пользователь уточнил\n" +
            "- constraints: ограничения или требования\n" +
            "- terms: зафиксированные термины или определения\n" +
            "Если факт не изменился — оставь как есть. Если нового нет — не добавляй.\n" +
            "Если никаких фактов не найдено — верни пустую строку.";

    private final AnthropicClient anthropicClient;

    /**
     * Анализирует последний обмен и возвращает обновлённый блок фактов.
     *
     * @param question     вопрос пользователя
     * @param answer       ответ ассистента
     * @param currentFacts текущая память задачи (может быть null)
     * @return обновлённый блок фактов или null если новых фактов нет
     */
    public String extract(String question, String answer, String currentFacts) {
        String currentBlock = (currentFacts != null && !currentFacts.isBlank())
                ? "Текущая память:\n" + currentFacts
                : "Текущая память: (пусто)";

        String userMessage = currentBlock + "\n\nПоследний обмен:\nUser: " + question
                + "\nAssistant: " + truncate(answer, 500);

        AnthropicRequest request = new AnthropicRequest(
                HAIKU_MODEL, 200, SYSTEM_PROMPT,
                null, null,
                List.of(new AnthropicRequest.Message("user", userMessage))
        );

        String result = anthropicClient.callApi(request).text().strip();
        log.debug("Memory extractor result: {}", result);
        return result.isBlank() ? currentFacts : result;
    }

    private String truncate(String text, int max) {
        return text != null && text.length() > max ? text.substring(0, max) + "..." : text;
    }
}
