package io.book.ai.handler.agent;

import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Извлекает долговременные факты о пользователе из рабочей памяти сессии.
 * Источник — рабочая память (весь накопленный контекст сессии), а не отдельный обмен.
 * Сохраняет только стабильные cross-session факты: профиль, явные решения пользователя.
 */
@Component
@RequiredArgsConstructor
public class AgentLongTermMemoryExtractor {

    private static final String HAIKU_MODEL = "claude-haiku-4-5-20251001";

    private final AnthropicClient anthropicClient;

    /**
     * @param workingMemory рабочая память сессии (key: value строки), агрегирует всю сессию
     * @param existingFacts текущая долговременная память в формате {@code category|key: value}, либо {@code null}
     * @return обновлённый блок фактов в формате {@code category|key: value} на каждой строке
     */
    public String extract(String workingMemory, String existingFacts) {
        String userContent = existingFacts != null
                ? "Existing long-term memory:\n" + existingFacts + "\n\nCurrent session working memory:\n" + workingMemory
                : "Current session working memory:\n" + workingMemory;

        AnthropicRequest req = new AnthropicRequest(
                HAIKU_MODEL, 400,
                """
                You extract facts about the USER that are worth remembering across multiple sessions.

                Categories (use ONLY these):
                - profile: who the user is — name, profession, language, location, stable preferences
                - decision: explicit choices the user committed to (architecture, tools, approach)

                What to store:
                - User's name if mentioned
                - User's profession or role
                - User's explicit long-term preferences or constraints
                - Explicit decisions the user made about their project/work

                What NOT to store:
                - Task details, requirements, or specifications (these are session-specific)
                - Information about code, books, or other artifacts being worked on
                - Assistant's suggestions or explanations
                - Anything that only applies to the current task

                Rules:
                - Output ONLY lines in format: category|key: value
                - Copy existing entries unchanged unless working memory explicitly updates them.
                - If nothing qualifies, output existing entries as-is.
                - No explanations, no blank lines, no headers.""",
                null, null,
                List.of(new Message("user", userContent))
        );

        return anthropicClient.callApi(req).text();
    }
}
