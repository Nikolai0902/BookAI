package io.book.ai.repository;

import io.book.ai.repository.entity.AgentMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMessageRepository extends JpaRepository<AgentMessageEntity, Long> {

    List<AgentMessageEntity> findBySessionIdOrderByCreatedAt(String sessionId);
}
