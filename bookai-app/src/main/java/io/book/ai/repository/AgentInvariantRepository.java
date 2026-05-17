package io.book.ai.repository;

import io.book.ai.repository.entity.AgentInvariantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий инвариантов профиля.
 */
public interface AgentInvariantRepository extends JpaRepository<AgentInvariantEntity, Long> {

    /** Возвращает все инварианты профиля, отсортированные по категории. */
    List<AgentInvariantEntity> findByProfileIdOrderByCategory(String profileId);

    /** Удаляет все инварианты профиля. */
    void deleteByProfileId(String profileId);
}
