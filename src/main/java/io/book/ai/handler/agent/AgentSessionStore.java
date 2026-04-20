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
}
