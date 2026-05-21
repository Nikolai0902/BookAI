package io.book.ai.scheduler.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Напоминание, сохранённое агентом через MCP-инструмент {@code addReminder}.
 */
@Entity
@Table(name = "reminders")
@Getter
@NoArgsConstructor
public class ReminderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime fireAt;

    private LocalDateTime firedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderStatus status;

    public ReminderEntity(String text, LocalDateTime createdAt, LocalDateTime fireAt) {
        this.text = text;
        this.createdAt = createdAt;
        this.fireAt = fireAt;
        this.status = ReminderStatus.PENDING;
    }

    /**
     * Переводит напоминание в статус {@link ReminderStatus#FIRED} и фиксирует время срабатывания.
     */
    public void fire(LocalDateTime firedAt) {
        this.status = ReminderStatus.FIRED;
        this.firedAt = firedAt;
    }
}
