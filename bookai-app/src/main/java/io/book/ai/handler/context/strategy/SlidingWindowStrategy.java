package io.book.ai.handler.context.strategy;

import io.book.ai.handler.agent.AgentSessionStore;
import io.book.ai.handler.context.ContextResult;
import io.book.ai.handler.context.ContextStrategy;
import io.book.ai.handler.context.ContextStrategyType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Стратегия скользящего окна: в LLM передаются только последние {@code windowSize} сообщений.
 * <p>
 * Все сообщения по-прежнему сохраняются в базе, однако при формировании контекста
 * старые сообщения отбрасываются без суммаризации. Это обеспечивает постоянный
 * размер входного контекста независимо от длины диалога.
 * <p>
 * Размер окна задаётся через свойство {@code agent.context.sliding-window-size}
 * (по умолчанию 10 сообщений).
 */
@Component
@RequiredArgsConstructor
public class SlidingWindowStrategy implements ContextStrategy {

    private final AgentSessionStore sessionStore;

    @Value("${agent.context.sliding-window-size:10}")
    private final int windowSize;

    @Override
    public ContextStrategyType type() {
        return ContextStrategyType.SLIDING_WINDOW;
    }

    /**
     * Загружает полную историю и возвращает подсписок из последних {@code windowSize} сообщений.
     * Если история короче окна, возвращаются все сообщения.
     */
    @Override
    public ContextResult buildContext(String sessionId, String model) {
        var all = sessionStore.loadHistory(sessionId);
        int from = Math.max(0, all.size() - windowSize);
        var window = all.subList(from, all.size());
        return new ContextResult(null, window, window.size(), 0);
    }
}
