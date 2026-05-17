package io.book.ai.repository;

import io.book.ai.repository.entity.AgentBranchEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий метаданных веток диалога для стратегии
 * {@link io.book.ai.handler.context.ContextStrategyType#BRANCHING}.
 * <p>
 * Хранит информацию о созданных ветках: от какой сессии и в какой точке они ответвились.
 * Сами сообщения веток находятся в таблице {@code agent_messages} под
 * соответствующим {@code branchSessionId}.
 */
public interface AgentBranchRepository extends JpaRepository<AgentBranchEntity, Long> {

    /**
     * Возвращает все ветки, созданные из указанной корневой сессии,
     * упорядоченные по времени создания.
     *
     * @param rootSessionId идентификатор родительской сессии
     * @return список веток в хронологическом порядке; пустой список если веток нет
     */
    List<AgentBranchEntity> findByRootSessionIdOrderByCreatedAt(String rootSessionId);

    /**
     * Ищет запись ветки по её идентификатору сессии.
     * Используется для определения, является ли входящий {@code sessionId} веткой
     * при формировании контекста в {@link io.book.ai.handler.agent.AgentSessionStore#loadHistoryForBranch}.
     *
     * @param branchSessionId UUID ветки
     * @return метаданные ветки, либо {@link Optional#empty()} если сессия не является веткой
     */
    Optional<AgentBranchEntity> findByBranchSessionId(String branchSessionId);
}
