package io.book.ai.handler.context.strategy;

import io.book.ai.handler.agent.AgentSessionStore;
import io.book.ai.handler.context.ContextResult;
import io.book.ai.handler.context.ContextStrategy;
import io.book.ai.handler.context.ContextStrategyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Стратегия «полная история»: в LLM передаются все сообщения сессии без каких-либо
 * ограничений или преобразований.
 * <p>
 * Наиболее простой вариант: контекст растёт с каждым ходом, поэтому стоимость
 * запроса увеличивается линейно. Подходит для коротких диалогов или случаев,
 * когда важна абсолютная точность всей истории.
 */
@Component
@RequiredArgsConstructor
public class FullHistoryStrategy implements ContextStrategy {

    private final AgentSessionStore sessionStore;

    @Override
    public ContextStrategyType type() {
        return ContextStrategyType.FULL_HISTORY;
    }

    /**
     * Загружает полную историю сессии и возвращает её без изменений.
     * Системный промпт не формируется.
     */
    @Override
    public ContextResult buildContext(String sessionId, String model) {
        var messages = sessionStore.loadHistory(sessionId);
        return new ContextResult(null, messages, 0, 0);
    }
}
