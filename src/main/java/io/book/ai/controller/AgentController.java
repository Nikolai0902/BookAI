package io.book.ai.controller;

import io.book.ai.api.AgentChatRequest;
import io.book.ai.api.AgentChatResponse;
import io.book.ai.api.branch.BranchCreateRequest;
import io.book.ai.api.branch.BranchInfo;
import io.book.ai.handler.agent.AgentBook;
import io.book.ai.handler.agent.AgentSessionStore;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentBook agentBook;
    private final AgentSessionStore sessionStore;

    @PostMapping("/chat")
    public AgentChatResponse chat(@RequestBody AgentChatRequest request) {
        return agentBook.chat(request);
    }

    @PostMapping("/branch")
    public BranchInfo createBranch(@RequestBody BranchCreateRequest request) {
        return sessionStore.createBranch(request.rootSessionId(), request.checkpointMessageId(), request.label());
    }

    @GetMapping("/branch/{rootSessionId}")
    public List<BranchInfo> listBranches(@PathVariable String rootSessionId) {
        return sessionStore.listBranches(rootSessionId);
    }
}
