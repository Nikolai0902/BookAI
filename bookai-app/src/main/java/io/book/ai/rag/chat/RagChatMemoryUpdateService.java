package io.book.ai.rag.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Асинхронно обновляет память задачи после каждого хода — не блокирует HTTP-ответ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatMemoryUpdateService {

    private final RagChatMemoryExtractor memoryExtractor;
    private final RagChatSessionStore sessionStore;

    /**
     * Извлекает факты из последнего обмена и сохраняет их в фоне.
     *
     * @param sessionId    идентификатор сессии
     * @param question     вопрос пользователя
     * @param answer       ответ ассистента
     * @param currentFacts текущая память задачи
     */
    @Async
    public void updateAsync(String sessionId, String question, String answer, String currentFacts) {
        try {
            log.info("RagChat [{}]: memory extraction started (async)", sessionId);
            String newFacts = memoryExtractor.extract(question, answer, currentFacts);
            if (newFacts != null && !newFacts.isBlank()) {
                sessionStore.saveContextFacts(sessionId, newFacts);
                log.info("RagChat [{}]: memory updated", sessionId);
            }
        } catch (Exception e) {
            log.warn("RagChat [{}]: memory extraction failed: {}", sessionId, e.getMessage());
        }
    }
}
