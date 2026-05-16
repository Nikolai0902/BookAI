package io.book.ai.handler.agent;

import io.book.ai.api.MemoryLayersSnapshot;
import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.repository.UserProfileRepository;
import io.book.ai.repository.entity.AgentLongTermMemoryEntity;
import io.book.ai.repository.entity.ResponseFormat;
import io.book.ai.repository.entity.UserProfileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Управляет тремя слоями памяти агента. Работает поверх любой контекстной стратегии.
 * <ul>
 *   <li>Краткосрочная — сырые сообщения в {@code agent_messages} (не изменяется).</li>
 *   <li>Рабочая — ключевые факты сессии в {@code agent_session_facts}.</li>
 *   <li>Долговременная — cross-session профиль/решения/знания в {@code agent_long_term_memory}.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AgentMemoryManager {

    public static final String DEFAULT_PROFILE = "default";

    private final AgentSessionStore sessionStore;
    private final FactsExtractor factsExtractor;
    private final AgentLongTermMemoryExtractor longTermExtractor;
    private final UserProfileRepository profileRepository;

    @Value("${agent.memory.long-term-update-interval:3}")
    private int longTermUpdateInterval;

    /**
     * Формирует блок предпочтений профиля для system prompt.
     * Вызывается всегда, независимо от флага memoryEnabled.
     * Возвращает {@code null} если профиль не найден.
     */
    public String buildProfileSystemPrompt(String profileId) {
        return profileRepository.findById(profileId).map(p -> {
            StringBuilder sb = new StringBuilder();
            sb.append("[User Profile: ").append(p.getDisplayName()).append("]\n");
            sb.append("Communication style: ").append(p.getStyle().name().toLowerCase()).append("\n");
            sb.append("Response format: ").append(formatResponseFormat(p.getResponseFormat())).append("\n");
            if (StringUtils.hasText(p.getConstraints())) {
                sb.append("Constraints: ").append(p.getConstraints()).append("\n");
            }
            return sb.toString();
        }).orElse(null);
    }

    /**
     * Формирует блок рабочей и долговременной памяти для system prompt.
     * Вызывается только когда memoryEnabled=true.
     * Возвращает {@code null} если обе памяти пусты.
     */
    public String buildMemoryLayersPrompt(String sessionId, String profileId) {
        String working = sessionStore.getFacts(sessionId);
        List<AgentLongTermMemoryEntity> ltm = sessionStore.getLongTermMemory(profileId);

        StringBuilder sb = new StringBuilder();

        if (!ltm.isEmpty()) {
            sb.append("## Долговременная память\n");
            Map<String, List<AgentLongTermMemoryEntity>> byCategory = ltm.stream()
                    .collect(Collectors.groupingBy(AgentLongTermMemoryEntity::getCategory,
                            LinkedHashMap::new, Collectors.toList()));
            byCategory.forEach((cat, entries) -> {
                sb.append("[").append(cat).append("]\n");
                entries.forEach(e -> sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n"));
            });
        }

        if (StringUtils.hasText(working)) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append("## Рабочая память (текущая сессия)\n").append(working);
        }

        return sb.isEmpty() ? null : sb.toString();
    }

    /**
     * Обновляет рабочую и долговременную память по последнему обмену.
     * Вызывается после сохранения ответа ассистента.
     *
     * @param lastExchange последние два сообщения: user + assistant
     * @return снимок всех трёх слоёв памяти для включения в ответ
     */
    public MemoryLayersSnapshot updateMemory(String sessionId, String profileId, List<Message> lastExchange) {
        updateWorkingMemory(sessionId, lastExchange);
        long turnNumber = sessionStore.getMessageCount(sessionId) / 2;
        if (turnNumber % longTermUpdateInterval == 0) {
            String workingMemory = sessionStore.getFacts(sessionId);
            if (StringUtils.hasText(workingMemory)) {
                updateLongTermMemory(profileId, workingMemory);
            }
        }
        return buildSnapshot(sessionId, profileId);
    }

    private void updateWorkingMemory(String sessionId, List<Message> lastExchange) {
        String existing = sessionStore.getFacts(sessionId);
        String updated = factsExtractor.extract(lastExchange, existing);
        if (StringUtils.hasText(updated)) {
            sessionStore.saveFacts(sessionId, updated);
        }
    }

    private void updateLongTermMemory(String profileId, String workingMemory) {
        List<AgentLongTermMemoryEntity> current = sessionStore.getLongTermMemory(profileId);
        String existingFormatted = current.isEmpty() ? null : current.stream()
                .map(e -> e.getCategory() + "|" + e.getKey() + ": " + e.getValue())
                .collect(Collectors.joining("\n"));

        String extracted = longTermExtractor.extract(workingMemory, existingFormatted);
        if (!StringUtils.hasText(extracted)) return;

        Arrays.stream(extracted.split("\n"))
                .map(String::trim)
                .filter(line -> line.contains("|") && line.contains(": "))
                .forEach(line -> {
                    String[] parts = line.split("\\|", 2);
                    String[] kv = parts[1].split(": ", 2);
                    if (parts.length == 2 && kv.length == 2) {
                        sessionStore.upsertLongTermFact(
                                profileId, parts[0].trim(), kv[0].trim(), kv[1].trim());
                    }
                });
    }

    public MemoryLayersSnapshot buildSnapshot(String sessionId, String profileId) {
        int shortTermCount = (int) sessionStore.getMessageCount(sessionId);
        String working = sessionStore.getFacts(sessionId);
        List<AgentLongTermMemoryEntity> ltm = sessionStore.getLongTermMemory(profileId);

        Map<String, String> longTermByCategory = ltm.stream()
                .collect(Collectors.groupingBy(
                        AgentLongTermMemoryEntity::getCategory,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                e -> e.getKey() + ": " + e.getValue(),
                                Collectors.joining("\n"))));

        return new MemoryLayersSnapshot(shortTermCount, working, longTermByCategory);
    }

    private String formatResponseFormat(ResponseFormat format) {
        return switch (format) {
            case PLAIN -> "plain text";
            case MARKDOWN -> "markdown";
            case BULLETS -> "bullet points";
            case DETAILED -> "detailed explanations";
        };
    }
}
