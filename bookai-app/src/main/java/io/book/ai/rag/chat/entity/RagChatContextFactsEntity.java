package io.book.ai.rag.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Запись таблицы {@code rag_chat_context_facts}.
 * Хранит память задачи для одной RAG-чат сессии в формате {@code key: value}.
 */
@Entity
@Table(name = "rag_chat_context_facts")
@Getter
@NoArgsConstructor
public class RagChatContextFactsEntity {

    @Id
    @Column(nullable = false)
    private String sessionId;

    /** Блок фактов в формате «key: value», по одной паре на строку. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String facts;

    @Column(nullable = false)
    private Instant updatedAt;

    public RagChatContextFactsEntity(String sessionId, String facts) {
        this.sessionId = sessionId;
        this.facts = facts;
        this.updatedAt = Instant.now();
    }

    /**
     * Обновляет блок фактов и время изменения.
     *
     * @param newFacts новое содержимое блока фактов
     */
    public void updateFacts(String newFacts) {
        this.facts = newFacts;
        this.updatedAt = Instant.now();
    }
}
