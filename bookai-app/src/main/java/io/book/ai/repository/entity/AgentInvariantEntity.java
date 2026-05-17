package io.book.ai.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Инвариант профиля — жёсткое правило, которое ассистент обязан соблюдать.
 * <p>
 * В отличие от поля {@code constraints} профиля (мягкие стилевые инструкции),
 * инварианты описывают непреодолимые ограничения: архитектурные решения, выбранный
 * стек, бизнес-правила. При конфликте запроса с инвариантом ассистент обязан
 * отказаться выполнять нарушающую часть.
 */
@Entity
@Table(name = "agent_invariants")
@Getter
@NoArgsConstructor
public class AgentInvariantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Идентификатор профиля-владельца инварианта. */
    @Column(nullable = false)
    private String profileId;

    /**
     * Категория инварианта: {@code архитектура}, {@code стек}, {@code бизнес}, {@code технические}.
     * Используется для группировки в system prompt.
     */
    @Column(nullable = false, length = 50)
    private String category;

    /** Текст правила. Передаётся LLM как обязательное ограничение. */
    @Column(name = "rule_text", nullable = false, columnDefinition = "TEXT")
    private String ruleText;

    @Column(nullable = false)
    private Instant createdAt;

    public AgentInvariantEntity(String profileId, String category, String ruleText) {
        this.profileId = profileId;
        this.category = category;
        this.ruleText = ruleText;
        this.createdAt = Instant.now();
    }

    /** Обновляет категорию и текст правила. */
    public void update(String category, String ruleText) {
        this.category = category;
        this.ruleText = ruleText;
    }
}
