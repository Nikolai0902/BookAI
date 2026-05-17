package io.book.ai.controller;

import io.book.ai.api.TaskStateSnapshot;
import io.book.ai.handler.task.AgentTaskStateManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/task")
@RequiredArgsConstructor
public class TaskStateController {

    private final AgentTaskStateManager taskStateManager;

    @GetMapping("/{sessionId}")
    public TaskStateSnapshot getState(@PathVariable String sessionId) {
        return taskStateManager.buildSnapshot(sessionId);
    }

    @PutMapping("/{sessionId}/pause")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void pause(@PathVariable String sessionId) {
        taskStateManager.pause(sessionId);
    }

    @PutMapping("/{sessionId}/resume")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resume(@PathVariable String sessionId) {
        taskStateManager.resume(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@PathVariable String sessionId) {
        taskStateManager.reset(sessionId);
    }
}
