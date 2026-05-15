package io.book.ai.handler.agent;

import io.book.ai.api.MemoryLayersSnapshot;
import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.repository.entity.AgentLongTermMemoryEntity;
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

    @Value("${agent.memory.long-term-update-interval:7}")
    private int longTermUpdateInterval;

    /**
     * Формирует блок памяти для system prompt. Вызывается до обращения к LLM.
     * Возвращает {@code null} если обе памяти пусты.
     */
    public String buildMemorySystemPrompt(String sessionId) {
        String working = sessionStore.getFacts(sessionId);
        List<AgentLongTermMemoryEntity> ltm = sessionStore.getLongTermMemory(DEFAULT_PROFILE);

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
    public MemoryLayersSnapshot updateMemory(String sessionId, List<Message> lastExchange) {
        updateWorkingMemory(sessionId, lastExchange);
        long turnNumber = sessionStore.getMessageCount(sessionId) / 2;
        if (turnNumber % longTermUpdateInterval == 0) {
            // Читаем рабочую память ПОСЛЕ её обновления — она суммирует всю сессию
            String workingMemory = sessionStore.getFacts(sessionId);
            if (StringUtils.hasText(workingMemory)) {
                updateLongTermMemory(workingMemory);
            }
        }
        return buildSnapshot(sessionId);
    }

    private void updateWorkingMemory(String sessionId, List<Message> lastExchange) {
        String existing = sessionStore.getFacts(sessionId);
        String updated = factsExtractor.extract(lastExchange, existing);
        if (StringUtils.hasText(updated)) {
            sessionStore.saveFacts(sessionId, updated);
        }
    }

    private void updateLongTermMemory(String workingMemory) {
        List<AgentLongTermMemoryEntity> current = sessionStore.getLongTermMemory(DEFAULT_PROFILE);
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
                                DEFAULT_PROFILE, parts[0].trim(), kv[0].trim(), kv[1].trim());
                    }
                });
    }

    public MemoryLayersSnapshot buildSnapshot(String sessionId) {
        int shortTermCount = (int) sessionStore.getMessageCount(sessionId);
        String working = sessionStore.getFacts(sessionId);
        List<AgentLongTermMemoryEntity> ltm = sessionStore.getLongTermMemory(DEFAULT_PROFILE);

        Map<String, String> longTermByCategory = ltm.stream()
                .collect(Collectors.groupingBy(
                        AgentLongTermMemoryEntity::getCategory,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                e -> e.getKey() + ": " + e.getValue(),
                                Collectors.joining("\n"))));

        return new MemoryLayersSnapshot(shortTermCount, working, longTermByCategory);
    }
}
