package io.book.ai.handler.agent;

import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.repository.AgentMessageRepository;
import io.book.ai.repository.entity.AgentMessageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AgentSessionStore {

    private final AgentMessageRepository repository;

    public List<Message> loadHistory(String sessionId) {
        return repository.findBySessionIdOrderByCreatedAt(sessionId).stream()
                .map(e -> new Message(e.getRole(), e.getContent()))
                .toList();
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
        return repository.countBySessionId(sessionId);
    }


}
