package io.book.ai.handler.context.strategy.sticky;

import io.book.ai.handler.agent.AgentSessionStore;
import io.book.ai.handler.context.ContextResult;
import io.book.ai.handler.context.ContextStrategy;
import io.book.ai.handler.context.ContextStrategyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Стратегия «ключевые факты»: совмещает компактный блок фактов о диалоге
 * с последними N сообщениями.
 * <p>
 * Принцип работы:
 * <ol>
 *   <li>Перед каждым ходом в системный промпт добавляется актуальный блок фактов
 *       в формате «key: value», извлечённых из предыдущих сообщений.</li>
 *   <li>В LLM передаются только последние {@code recentCount} сообщений.</li>
 *   <li>После получения ответа блок фактов обновляется через {@link FactsExtractor}
 *       отдельным LLM-вызовом (модель Haiku для экономии).</li>
 * </ol>
 * Количество «живых» сообщений задаётся через {@code agent.context.recent-messages-count}
 * (по умолчанию 5).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StickyFactsStrategy implements ContextStrategy {

    private static final String FACTS_PREFIX = "## Key Facts\n";

    private final AgentSessionStore sessionStore;
    private final FactsExtractor factsExtractor;

    @Value("${agent.context.recent-messages-count:5}")
    private final int recentCount;

    @Override
    public ContextStrategyType type() {
        return ContextStrategyType.STICKY_FACTS;
    }

    /**
     * Формирует контекст из последних {@code recentCount} сообщений.
     * Если для сессии сохранены факты, они добавляются в системный промпт
     * под заголовком {@code ## Key Facts}.
     */
    @Override
    public ContextResult buildContext(String sessionId, String model) {
        var all = sessionStore.loadHistory(sessionId);
        int from = Math.max(0, all.size() - recentCount);
        var recent = all.subList(from, all.size());

        String facts = sessionStore.getFacts(sessionId);
        String systemPrompt = facts != null ? FACTS_PREFIX + facts : null;

        return new ContextResult(systemPrompt, recent, recent.size(), 0);
    }

    /**
     * Обновляет блок ключевых фактов после каждого хода.
     * <p>
     * При первом вызове (фактов ещё нет) передаётся вся история.
     * При последующих вызовах — только последние два сообщения (ход user + ответ assistant),
     * поскольку существующие факты уже охватывают более раннюю часть диалога.
     * Это снижает стоимость обновления с O(n) до O(1) токенов за ход.
     * <p>
     * При ошибке извлечения (например, сбой сети) старый блок сохраняется,
     * а исключение логируется как предупреждение — сессия не прерывается.
     *
     * @return обновлённая строка с фактами, либо {@code null} при ошибке
     */
    @Override
    public String afterLlmResponse(String sessionId, String model) {
        try {
            var history = sessionStore.loadHistory(sessionId);
            String existing = sessionStore.getFacts(sessionId);
            // When updating, only the latest exchange is new — facts already cover earlier turns
            var toExtract = existing != null && history.size() > 2
                    ? history.subList(history.size() - 2, history.size())
                    : history;
            String updated = factsExtractor.extract(toExtract, existing);
            sessionStore.saveFacts(sessionId, updated);
            return updated;
        } catch (Exception e) {
            log.warn("Failed to update facts for session {}", sessionId, e);
            return null;
        }
    }
}
