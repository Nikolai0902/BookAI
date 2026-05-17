package io.book.ai.repository.entity;

import io.book.ai.handler.task.TaskPhase;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "agent_task_state")
@Getter
@NoArgsConstructor
public class AgentTaskStateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPhase phase;

    @Column(columnDefinition = "TEXT")
    private String currentStep;

    @Column(columnDefinition = "TEXT")
    private String expectedAction;

    @Column(nullable = false)
    private boolean paused;

    @Column(nullable = false)
    private boolean planApproved;

    @Column(columnDefinition = "TEXT")
    private String blockedTransitionReason;

    @Column(nullable = false)
    private Instant updatedAt;

    public AgentTaskStateEntity(String sessionId, TaskPhase phase, String currentStep, String expectedAction) {
        this.sessionId = sessionId;
        this.phase = phase;
        this.currentStep = currentStep;
        this.expectedAction = expectedAction;
        this.paused = false;
        this.planApproved = false;
        this.updatedAt = Instant.now();
    }

    public void update(TaskPhase phase, String currentStep, String expectedAction) {
        this.phase = phase;
        this.currentStep = currentStep;
        this.expectedAction = expectedAction;
        this.blockedTransitionReason = null;
        this.updatedAt = Instant.now();
    }

    public void approvePlan() {
        this.planApproved = true;
        this.updatedAt = Instant.now();
    }

    /** Фиксирует причину отклонённого перехода без изменения фазы. */
    public void block(String reason) {
        this.blockedTransitionReason = reason;
        this.updatedAt = Instant.now();
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
        this.updatedAt = Instant.now();
    }
}
