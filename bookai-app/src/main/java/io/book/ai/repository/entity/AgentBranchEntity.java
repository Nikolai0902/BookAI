package io.book.ai.repository.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Запись таблицы {@code agent_branches}.
 * <p>
 * Описывает одну ветку диалога, созданную из точки ветвления в родительской сессии.
 * Собственные сообщения ветки хранятся в таблице {@code agent_messages} под
 * идентификатором {@code branchSessionId}, как обычная сессия.
 * <p>
 * При формировании контекста {@link io.book.ai.handler.agent.AgentSessionStore#loadHistoryForBranch}
 * объединяет сообщения родителя (до {@code checkpointMessageId} включительно)
 * с сообщениями самой ветки.
 */
@Entity
@Table(name = "agent_branches")
@Getter
@NoArgsConstructor
public class AgentBranchEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID ветки; используется как {@code sessionId} для всех сообщений ветки. */
    @Column(nullable = false, unique = true)
    private String branchSessionId;

    /** {@code sessionId} родительской сессии, от которой создана ветка. */
    @Column(nullable = false)
    private String rootSessionId;

    /**
     * Идентификатор последнего сообщения родительской сессии, включённого в контекст ветки.
     * Соответствует полю {@code id} записи в таблице {@code agent_messages}.
     */
    @Column(nullable = false)
    private Long checkpointMessageId;

    /** Пользовательское название ветки. */
    @Column(nullable = false)
    private String label;

    /** Время создания ветки. */
    @Column(nullable = false)
    private Instant createdAt;

    /**
     * Создаёт метаданные новой ветки диалога.
     *
     * @param branchSessionId    UUID новой ветки
     * @param rootSessionId      идентификатор родительской сессии
     * @param checkpointMessageId идентификатор сообщения-точки ветвления
     * @param label              пользовательское название ветки
     */
    public AgentBranchEntity(String branchSessionId, String rootSessionId,
                              Long checkpointMessageId, String label) {
        this.branchSessionId = branchSessionId;
        this.rootSessionId = rootSessionId;
        this.checkpointMessageId = checkpointMessageId;
        this.label = label;
        this.createdAt = Instant.now();
    }
}
