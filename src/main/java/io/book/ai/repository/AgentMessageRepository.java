package io.book.ai.repository;

import io.book.ai.repository.entity.AgentMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AgentMessageRepository extends JpaRepository<AgentMessageEntity, Long> {

    List<AgentMessageEntity> findBySessionIdOrderByCreatedAt(String sessionId);

    @Query("SELECT COALESCE(SUM(m.inputTokens), 0) FROM AgentMessageEntity m WHERE m.sessionId = :sessionId")
    int sumInputTokensBySessionId(String sessionId);

    @Query("SELECT COALESCE(SUM(m.outputTokens), 0) FROM AgentMessageEntity m WHERE m.sessionId = :sessionId")
    int sumOutputTokensBySessionId(String sessionId);

    long countBySessionId(String sessionId);
}
