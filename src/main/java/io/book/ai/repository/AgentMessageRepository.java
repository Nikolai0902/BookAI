package io.book.ai.repository;

import io.book.ai.repository.entity.AgentMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий сообщений диалоговых сессий.
 * <p>
 * Хранит как обычные сообщения (user/assistant), так и записи-саммари
 * ({@code is_summary = true}). Методы с суффиксом {@code SummaryFalse}
 * работают только с реальными сообщениями, исключая саммари.
 */
public interface AgentMessageRepository extends JpaRepository<AgentMessageEntity, Long> {

    List<AgentMessageEntity> findBySessionIdOrderByCreatedAt(String sessionId);

    List<AgentMessageEntity> findBySessionIdAndSummaryFalseOrderByCreatedAt(String sessionId);

    Optional<AgentMessageEntity> findTopBySessionIdAndSummaryTrueOrderByCreatedAtDesc(String sessionId);

    void deleteBySessionIdAndSummaryTrue(String sessionId);

    @Query("SELECT COALESCE(SUM(m.inputTokens), 0) FROM AgentMessageEntity m WHERE m.sessionId = :sessionId")
    int sumInputTokensBySessionId(String sessionId);

    @Query("SELECT COALESCE(SUM(m.outputTokens), 0) FROM AgentMessageEntity m WHERE m.sessionId = :sessionId")
    int sumOutputTokensBySessionId(String sessionId);

    long countBySessionId(String sessionId);

    long countBySessionIdAndSummaryFalse(String sessionId);
}
