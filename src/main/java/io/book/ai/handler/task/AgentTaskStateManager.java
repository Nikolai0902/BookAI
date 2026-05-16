package io.book.ai.handler.task;

import io.book.ai.api.TaskStateSnapshot;
import io.book.ai.repository.AgentTaskStateRepository;
import io.book.ai.repository.entity.AgentTaskStateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Менеджер состояния задачи в диалоге агента.
 * <p>
 * Отвечает за сохранение и проверку переходов между фазами жизненного цикла задачи,
 * формирование блока системного промпта с текущим состоянием и правилами переходов,
 * а также за сборку снапшота состояния для ответа клиенту.
 * <p>
 * Логика допустимых переходов определяется в {@link TaskPhase}.
 * Дополнительный условный шлюз — переход PLANNING→EXECUTION требует явного
 * подтверждения плана пользователем ({@code planApproved}).
 */
@Component
@RequiredArgsConstructor
public class AgentTaskStateManager {

    private final AgentTaskStateRepository repository;

    /**
     * Формирует блок системного промпта с текущим состоянием задачи и правилами переходов.
     * Включает предупреждение о последнем заблокированном переходе, если такой был.
     *
     * @param sessionId идентификатор сессии
     * @return строка блока или {@code null} если состояния нет
     */
    public String buildTaskStatePrompt(String sessionId) {
        return repository.findBySessionId(sessionId).map(e -> {
            StringBuilder sb = new StringBuilder();
            sb.append(e.isPaused() ? "## Task State [PAUSED]\n" : "## Task State\n");
            sb.append("Phase: ").append(e.getPhase().name().toLowerCase()).append("\n");
            if (StringUtils.hasText(e.getCurrentStep())) {
                sb.append("Current step: ").append(e.getCurrentStep()).append("\n");
            }
            String action = e.isPaused()
                    ? "Task is on hold. Do not advance the task. Acknowledge the pause if asked."
                    : e.getExpectedAction();
            if (StringUtils.hasText(action)) {
                sb.append("Expected action: ").append(action).append("\n");
            }

            sb.append("\n## Правила жизненного цикла задачи\n");
            sb.append("- Начало реализации (EXECUTION) требует явного подтверждения плана пользователем\n");
            sb.append("- Завершение задачи (DONE) требует прохождения валидации (VALIDATION)\n");
            sb.append("- Пропуск этапов запрещён: NONE→EXECUTION, PLANNING→DONE и т.д.\n");

            if (e.getPhase() == TaskPhase.PLANNING && !e.isPlanApproved()) {
                sb.append("- Ожидай подтверждения плана от пользователя перед началом реализации\n");
            }
            if (StringUtils.hasText(e.getBlockedTransitionReason())) {
                sb.append("- Последняя попытка перехода отклонена: ").append(e.getBlockedTransitionReason()).append("\n");
            }

            return sb.toString();
        }).orElse(null);
    }

    /**
     * Проверяет допустимость перехода и сохраняет новое состояние задачи.
     * <p>
     * Если {@code newPhase} равен {@code null} или {@code NONE} — обновление пропускается.
     * Если переход структурно запрещён или PLANNING→EXECUTION без подтверждения плана —
     * фаза не меняется, в сущности фиксируется причина блокировки.
     * <p>
     * Подтверждение плана ({@code planApproved=true}) применяется безусловно,
     * даже если итоговый переход оказывается заблокированным по другой причине.
     *
     * @param sessionId   идентификатор сессии
     * @param newPhase    целевая фаза; {@code null} — пропустить обновление
     * @param step        текущий шаг в рамках фазы
     * @param action      ожидаемое следующее действие
     * @param planApproved {@code true} если пользователь явно подтвердил план в данном ходе
     */
    @Transactional
    public void updateTaskState(String sessionId, TaskPhase newPhase, String step, String action, boolean planApproved) {
        if (newPhase == null || newPhase == TaskPhase.NONE) return;

        AgentTaskStateEntity entity = repository.findBySessionId(sessionId)
                .orElseGet(() -> new AgentTaskStateEntity(sessionId, newPhase, step, action));

        if (planApproved && entity.getPhase() == TaskPhase.PLANNING) {
            entity.approvePlan();
        }

        TaskPhase currentPhase = entity.getPhase();

        if (currentPhase == newPhase) {
            entity.update(newPhase, step, action);
            repository.save(entity);
            return;
        }

        if (!currentPhase.canTransitionTo(newPhase)) {
            entity.block("Переход с " + currentPhase.name() + " на " + newPhase.name() + " запрещён");
            repository.save(entity);
            return;
        }

        if (currentPhase == TaskPhase.PLANNING && newPhase == TaskPhase.EXECUTION && !entity.isPlanApproved()) {
            entity.block("Переход к реализации требует подтверждения плана пользователем");
            repository.save(entity);
            return;
        }

        entity.update(newPhase, step, action);
        repository.save(entity);
    }

    /**
     * Создаёт снапшот текущего состояния задачи для включения в ответ клиенту.
     *
     * @param sessionId идентификатор сессии
     * @return снапшот или {@code null} если состояния нет
     */
    public TaskStateSnapshot buildSnapshot(String sessionId) {
        return repository.findBySessionId(sessionId)
                .map(e -> new TaskStateSnapshot(
                        e.getPhase(),
                        e.getCurrentStep(),
                        e.getExpectedAction(),
                        e.isPaused(),
                        e.isPlanApproved(),
                        e.getBlockedTransitionReason()))
                .orElse(null);
    }

    @Transactional
    public void pause(String sessionId) {
        repository.findBySessionId(sessionId).ifPresent(e -> {
            e.setPaused(true);
            repository.save(e);
        });
    }

    @Transactional
    public void resume(String sessionId) {
        repository.findBySessionId(sessionId).ifPresent(e -> {
            e.setPaused(false);
            repository.save(e);
        });
    }

    @Transactional
    public void reset(String sessionId) {
        repository.findBySessionId(sessionId).ifPresent(repository::delete);
    }
}
