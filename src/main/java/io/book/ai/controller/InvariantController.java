package io.book.ai.controller;

import io.book.ai.api.AgentInvariantRequest;
import io.book.ai.handler.agent.AgentInvariantManager;
import io.book.ai.repository.entity.AgentInvariantEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD-контроллер инвариантов профиля.
 * Все операции делегируются {@link AgentInvariantManager}.
 */
@RestController
@RequestMapping("/api/profiles/{profileId}/invariants")
@RequiredArgsConstructor
public class InvariantController {

    private final AgentInvariantManager invariantManager;

    @GetMapping
    public List<AgentInvariantEntity> list(@PathVariable String profileId) {
        return invariantManager.getInvariants(profileId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentInvariantEntity add(@PathVariable String profileId,
                                    @RequestBody AgentInvariantRequest request) {
        return invariantManager.addInvariant(profileId, request.category(), request.ruleText());
    }

    @PutMapping("/{id}")
    public AgentInvariantEntity update(@PathVariable String profileId,
                                       @PathVariable Long id,
                                       @RequestBody AgentInvariantRequest request) {
        return invariantManager.updateInvariant(id, request.category(), request.ruleText());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String profileId, @PathVariable Long id) {
        invariantManager.removeInvariant(id);
    }
}
