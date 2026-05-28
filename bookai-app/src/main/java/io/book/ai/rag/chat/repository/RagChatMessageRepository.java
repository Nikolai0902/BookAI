package io.book.ai.rag.chat.repository;

import io.book.ai.rag.chat.entity.RagChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Репозиторий сообщений RAG-чата.
 */
public interface RagChatMessageRepository extends JpaRepository<RagChatMessageEntity, Long> {

    /**
     * Возвращает последние {@code limit} сообщений сессии в обратном хронологическом порядке.
     * Для получения истории в прямом порядке результат нужно перевернуть.
     */
    List<RagChatMessageEntity> findTop10BySessionIdOrderByCreatedAtDesc(String sessionId);

    /** Считает все сообщения сессии (для расчёта номера хода). */
    long countBySessionId(String sessionId);

    /** Удаляет все сообщения сессии. */
    void deleteBySessionId(String sessionId);
}
