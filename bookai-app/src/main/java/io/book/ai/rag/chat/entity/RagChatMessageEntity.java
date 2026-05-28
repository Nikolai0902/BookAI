package io.book.ai.rag.chat.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Запись таблицы {@code rag_chat_messages}.
 * Хранит одно сообщение RAG-чата: роль, текст, сериализованные цитаты и токен-статистику.
 */
@Entity
@Table(name = "rag_chat_messages")
@Getter
@NoArgsConstructor
public class RagChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    /** {@code "user"} или {@code "assistant"} */
    @Column(nullable = false)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    /** JSON-массив цитат (только для роли {@code "assistant"}), может быть null. */
    @Column(columnDefinition = "TEXT")
    private String citationsJson;

    private Integer inputTokens;
    private Integer outputTokens;

    @Column(nullable = false)
    private Instant createdAt;

    /** Конструктор для сообщения пользователя. */
    public RagChatMessageEntity(String sessionId, String role, String content) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.createdAt = Instant.now();
    }

    /** Конструктор для ответа ассистента с токенами и цитатами. */
    public RagChatMessageEntity(String sessionId, String content,
                                String citationsJson, int inputTokens, int outputTokens) {
        this.sessionId = sessionId;
        this.role = "assistant";
        this.content = content;
        this.citationsJson = citationsJson;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.createdAt = Instant.now();
    }
}
