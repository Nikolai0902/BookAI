package io.book.ai.controller;

import io.book.ai.handler.agent.AgentSessionStore;
import io.book.ai.handler.agent.AgentMemoryManager;
import io.book.ai.repository.entity.AgentLongTermMemoryEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final AgentSessionStore sessionStore;

    @GetMapping("/longterm")
    public List<AgentLongTermMemoryEntity> getLongTermMemory() {
        return sessionStore.getLongTermMemory(AgentMemoryManager.DEFAULT_PROFILE);
    }

    @DeleteMapping("/longterm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearLongTermMemory() {
        sessionStore.clearLongTermMemory(AgentMemoryManager.DEFAULT_PROFILE);
    }

    @GetMapping("/working/{sessionId}")
    public String getWorkingMemory(@PathVariable String sessionId) {
        String facts = sessionStore.getFacts(sessionId);
        return facts != null ? facts : "";
    }
}
