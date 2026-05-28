package io.book.ai.rag.chat.repository;

import io.book.ai.rag.chat.entity.RagChatContextFactsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий памяти задачи RAG-чата.
 */
public interface RagChatContextFactsRepository extends JpaRepository<RagChatContextFactsEntity, String> {

    Optional<RagChatContextFactsEntity> findBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
