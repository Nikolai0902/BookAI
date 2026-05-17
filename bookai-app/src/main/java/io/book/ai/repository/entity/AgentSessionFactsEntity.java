package io.book.ai.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Запись таблицы {@code agent_session_facts}.
 * <p>
 * Хранит блок ключевых фактов для одной сессии при использовании стратегии
 * {@link io.book.ai.handler.context.ContextStrategyType#STICKY_FACTS}.
 * На каждую сессию существует не более одной записи (уникальное поле {@code sessionId});
 * при обновлении фактов запись не заменяется, а изменяется на месте через {@link #updateFacts}.
 */
@Entity
@Table(name = "agent_session_facts")
@Getter
@NoArgsConstructor
public class AgentSessionFactsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Идентификатор сессии агента. Уникален в рамках таблицы. */
    @Column(nullable = false, unique = true)
    private String sessionId;

    /** Блок фактов в формате «key: value», по одной паре на строку. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String facts;

    /** Время последнего обновления блока фактов. */
    @Column(nullable = false)
    private Instant updatedAt;

    /**
     * Создаёт новую запись фактов для сессии.
     *
     * @param sessionId идентификатор сессии
     * @param facts     начальный блок фактов
     */
    public AgentSessionFactsEntity(String sessionId, String facts) {
        this.sessionId = sessionId;
        this.facts = facts;
        this.updatedAt = Instant.now();
    }

    /**
     * Обновляет блок фактов и проставляет текущее время как {@code updatedAt}.
     *
     * @param newFacts новое содержимое блока фактов
     */
    public void updateFacts(String newFacts) {
        this.facts = newFacts;
        this.updatedAt = Instant.now();
    }
}
