package io.book.ai.repository;

import io.book.ai.repository.entity.AgentSessionFactsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий блоков ключевых фактов для стратегии
 * {@link io.book.ai.handler.context.ContextStrategyType#STICKY_FACTS}.
 * <p>
 * На каждую сессию хранится не более одной записи. Обновление фактов
 * выполняется мутацией существующей записи через
 * {@link AgentSessionFactsEntity#updateFacts}, а не заменой строки в таблице.
 */
public interface AgentSessionFactsRepository extends JpaRepository<AgentSessionFactsEntity, Long> {

    /**
     * Возвращает блок фактов для указанной сессии, если он существует.
     *
     * @param sessionId идентификатор сессии
     * @return запись с фактами, либо {@link Optional#empty()} если фактов ещё нет
     */
    Optional<AgentSessionFactsEntity> findBySessionId(String sessionId);
}
