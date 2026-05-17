package io.book.ai.repository;

import io.book.ai.repository.entity.AgentTaskStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentTaskStateRepository extends JpaRepository<AgentTaskStateEntity, Long> {

    Optional<AgentTaskStateEntity> findBySessionId(String sessionId);
}
