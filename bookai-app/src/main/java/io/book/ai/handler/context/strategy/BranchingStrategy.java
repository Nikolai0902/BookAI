package io.book.ai.handler.context.strategy;

import io.book.ai.handler.agent.AgentSessionStore;
import io.book.ai.handler.context.ContextResult;
import io.book.ai.handler.context.ContextStrategy;
import io.book.ai.handler.context.ContextStrategyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Стратегия ветвления: поддерживает создание независимых веток диалога от произвольной точки.
 * <p>
 * При ветвлении создаётся новая сессия ({@code branchSessionId}), которая привязана
 * к родительской сессии ({@code rootSessionId}) и точке ветвления ({@code checkpointMessageId}).
 * История для LLM собирается из двух частей:
 * <ol>
 *   <li>Сообщения родительской сессии до точки ветвления включительно.</li>
 *   <li>Собственные сообщения текущей ветки.</li>
 * </ol>
 * Если {@code sessionId} не является веткой, поведение совпадает с {@link FullHistoryStrategy}.
 */
@Component
@RequiredArgsConstructor
public class BranchingStrategy implements ContextStrategy {

    private final AgentSessionStore sessionStore;

    @Override
    public ContextStrategyType type() {
        return ContextStrategyType.BRANCHING;
    }

    /**
     * Загружает историю с учётом линии предков: сначала сообщения корневой сессии
     * до точки ветвления, затем сообщения самой ветки.
     * Системный промпт не формируется.
     */
    @Override
    public ContextResult buildContext(String sessionId, String model) {
        var messages = sessionStore.loadHistoryForBranch(sessionId);
        return new ContextResult(null, messages, messages.size(), 0);
    }
}
