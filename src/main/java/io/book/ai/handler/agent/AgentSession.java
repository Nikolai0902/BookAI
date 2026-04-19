package io.book.ai.handler.agent;

import io.book.ai.llm.AnthropicRequest.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class AgentSession {

    @Getter
    private final String id;
    private final List<Message> history = new ArrayList<>();

    public List<Message> getHistory() {
        return List.copyOf(history);
    }

    public void addUserMessage(String text) {
        history.add(new Message("user", text));
    }

    public void addAssistantMessage(String text) {
        history.add(new Message("assistant", text));
    }
}
