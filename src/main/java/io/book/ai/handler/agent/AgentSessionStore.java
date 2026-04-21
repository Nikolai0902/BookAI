package io.book.ai.handler.agent;

import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.repository.AgentMessageRepository;
import io.book.ai.repository.entity.AgentMessageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Хранилище сообщений и саммари для диалоговых сессий.
 * <p>
 * Инкапсулирует все операции с {@link AgentMessageRepository}:
 * сохранение и загрузку сообщений, управление записью-саммари,
 * агрегацию статистики токенов по сессии.
 */
@Component
@RequiredArgsConstructor
public class AgentSessionStore {

    private final AgentMessageRepository repository;

    public List<Message> loadHistory(String sessionId) {
        return repository.findBySessionIdAndSummaryFalseOrderByCreatedAt(sessionId).stream()
                .map(e -> new Message(e.getRole(), e.getContent()))
                .toList();
    }

    public List<AgentMessageEntity> loadRawHistory(String sessionId) {
        return repository.findBySessionIdAndSummaryFalseOrderByCreatedAt(sessionId);
    }

    public AgentMessageEntity getSummary(String sessionId) {
        return repository.findTopBySessionIdAndSummaryTrueOrderByCreatedAtDesc(sessionId).orElse(null);
    }

    @Transactional
    public void saveSummary(String sessionId, String content, int coverCount) {
        repository.deleteBySessionIdAndSummaryTrue(sessionId);
        repository.save(new AgentMessageEntity(sessionId, "summary", content, true, coverCount));
    }

    public void saveMessage(String sessionId, String role, String content) {
        repository.save(new AgentMessageEntity(sessionId, role, content));
    }

    public void saveAssistantMessage(String sessionId, String content, int inputTokens, int outputTokens) {
        repository.save(new AgentMessageEntity(sessionId, "assistant", content, inputTokens, outputTokens));
    }

    public int getTotalInputTokens(String sessionId) {
        return repository.sumInputTokensBySessionId(sessionId);
    }

    public int getTotalOutputTokens(String sessionId) {
        return repository.sumOutputTokensBySessionId(sessionId);
    }

    public long getMessageCount(String sessionId) {
        return repository.countBySessionIdAndSummaryFalse(sessionId);
    }
}
