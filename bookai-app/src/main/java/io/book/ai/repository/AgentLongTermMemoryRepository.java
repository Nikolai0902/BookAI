package io.book.ai.repository;

import io.book.ai.repository.entity.AgentLongTermMemoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentLongTermMemoryRepository extends JpaRepository<AgentLongTermMemoryEntity, Long> {

    List<AgentLongTermMemoryEntity> findByProfileIdOrderByCategory(String profileId);

    Optional<AgentLongTermMemoryEntity> findByProfileIdAndKey(String profileId, String key);

    void deleteByProfileId(String profileId);
}
