package io.book.ai.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Запись в таблице {@code agent_messages}.
 * <p>
 * Хранит одно сообщение сессии: роль ({@code user} / {@code assistant}),
 * текст, время создания и токены ответа. Поле {@code summary = true}
 * обозначает специальную запись — саммари старых сообщений;
 * {@code summaryCoverCount} указывает, сколько реальных сообщений
 * оно охватывает.
 */
@Entity
@Table(name = "agent_messages")
@Getter
@NoArgsConstructor
public class AgentMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Instant createdAt;

    private Integer inputTokens;
    private Integer outputTokens;

    @Column(name = "is_summary", columnDefinition = "BOOLEAN DEFAULT FALSE NOT NULL")
    private boolean summary = false;

    @Column(name = "summary_cover_count")
    private Integer summaryCoverCount;

    public AgentMessageEntity(String sessionId, String role, String content) {
        this.sessionId = sessionId;
        this.role = role;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public AgentMessageEntity(String sessionId, String role, String content, int inputTokens, int outputTokens) {
        this(sessionId, role, content);
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
    }

    public AgentMessageEntity(String sessionId, String role, String content, boolean summary, Integer summaryCoverCount) {
        this(sessionId, role, content);
        this.summary = summary;
        this.summaryCoverCount = summaryCoverCount;
    }
}
