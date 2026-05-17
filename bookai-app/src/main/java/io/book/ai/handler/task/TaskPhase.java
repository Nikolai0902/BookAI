package io.book.ai.handler.task;

import java.util.Map;
import java.util.Set;

/**
 * Фазы жизненного цикла задачи в диалоге агента.
 * <p>
 * Допустимые переходы между фазами задаются статической картой {@code ALLOWED}.
 * Переход PLANNING→EXECUTION дополнительно требует подтверждения плана пользователем —
 * это условие проверяется в {@link AgentTaskStateManager}, а не здесь.
 */
public enum TaskPhase {
    NONE, PLANNING, EXECUTION, VALIDATION, DONE;

    private static final Map<TaskPhase, Set<TaskPhase>> ALLOWED = Map.of(
            NONE,       Set.of(PLANNING),
            PLANNING,   Set.of(PLANNING, EXECUTION),
            EXECUTION,  Set.of(PLANNING, EXECUTION, VALIDATION),
            VALIDATION, Set.of(PLANNING, EXECUTION, VALIDATION, DONE),
            DONE,       Set.of(NONE, PLANNING, EXECUTION, VALIDATION, DONE)
    );

    /**
     * Проверяет, допустим ли переход из текущей фазы в указанную.
     * Переход в {@code NONE} разрешён всегда (сброс состояния).
     * Переход PLANNING→EXECUTION проходит базовую проверку здесь,
     * но условие «план подтверждён» контролируется отдельно.
     *
     * @param to целевая фаза
     * @return {@code true} если переход структурно допустим
     */
    public boolean canTransitionTo(TaskPhase to) {
        return to == NONE || ALLOWED.getOrDefault(this, Set.of()).contains(to);
    }
}
