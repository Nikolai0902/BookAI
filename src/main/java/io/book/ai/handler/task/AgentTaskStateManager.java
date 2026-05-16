package io.book.ai.handler.task;

import io.book.ai.api.TaskStateSnapshot;
import io.book.ai.repository.AgentTaskStateRepository;
import io.book.ai.repository.entity.AgentTaskStateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AgentTaskStateManager {

    private final AgentTaskStateRepository repository;

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
            return sb.toString();
        }).orElse(null);
    }

    /**
     * Сохраняет состояние задачи на основе уже извлечённых значений.
     * Анализ последнего хода выполняется снаружи — {@link io.book.ai.handler.agent.LastExchangeAnalyzer}.
     * Если {@code phase} равен {@code null} или {@code NONE}, обновление пропускается,
     * чтобы разговорные ходы не сбрасывали накопленное состояние.
     *
     * @param sessionId идентификатор сессии
     * @param phase     определённая фаза задачи; {@code null} — пропустить обновление
     * @param step      текущий шаг в рамках фазы
     * @param action    ожидаемое следующее действие
     */
    @Transactional
    public void updateTaskState(String sessionId, TaskPhase phase, String step, String action) {
        if (phase == null || phase == TaskPhase.NONE) return;

        AgentTaskStateEntity entity = repository.findBySessionId(sessionId)
                .orElseGet(() -> new AgentTaskStateEntity(sessionId, phase, step, action));
        entity.update(phase, step, action);
        repository.save(entity);
    }

    public TaskStateSnapshot buildSnapshot(String sessionId) {
        return repository.findBySessionId(sessionId)
                .map(e -> new TaskStateSnapshot(
                        e.getPhase(), e.getCurrentStep(), e.getExpectedAction(), e.isPaused()))
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
