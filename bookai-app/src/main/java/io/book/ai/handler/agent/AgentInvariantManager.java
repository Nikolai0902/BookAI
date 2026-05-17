package io.book.ai.handler.agent;

import io.book.ai.repository.AgentInvariantRepository;
import io.book.ai.repository.entity.AgentInvariantEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Управляет инвариантами профиля: хранением, CRUD и инжектом в system prompt.
 * <p>
 * Инварианты — жёсткие правила, которые ассистент обязан соблюдать. Инжектируются
 * вторым слоем в system prompt (после блока профиля, перед памятью), чтобы LLM
 * видел их раньше контекста диалога и не мог их «забыть».
 */
@Component
@RequiredArgsConstructor
public class AgentInvariantManager {

    private final AgentInvariantRepository repository;

    /**
     * Формирует блок инвариантов для system prompt.
     * Возвращает {@code null} если у профиля нет инвариантов — {@code mergeSystemPrompts}
     * в {@link AgentBook} обрабатывает {@code null} без дополнительных проверок.
     *
     * @param profileId идентификатор профиля
     * @return форматированный блок с директивой или {@code null}
     */
    public String buildInvariantsSystemPrompt(String profileId) {
        List<AgentInvariantEntity> invariants = repository.findByProfileIdOrderByCategory(profileId);
        if (invariants.isEmpty()) return null;

        Map<String, List<AgentInvariantEntity>> byCategory = invariants.stream()
                .collect(Collectors.groupingBy(AgentInvariantEntity::getCategory,
                        LinkedHashMap::new, Collectors.toList()));

        StringBuilder sb = new StringBuilder();
        sb.append("## Инварианты (обязательные ограничения)\n");
        sb.append("Правила ниже НЕЛЬЗЯ нарушать. При конфликте с запросом — назови нарушаемый инвариант, объясни конфликт и откажись от нарушающей части.\n");
        byCategory.forEach((cat, entries) -> {
            sb.append("\n[").append(cat).append("]\n");
            entries.forEach(e -> sb.append("- ").append(e.getRuleText()).append("\n"));
        });
        return sb.toString();
    }

    /**
     * Возвращает все инварианты профиля, отсортированные по категории.
     *
     * @param profileId идентификатор профиля
     * @return список инвариантов; пустой список если инвариантов нет
     */
    public List<AgentInvariantEntity> getInvariants(String profileId) {
        return repository.findByProfileIdOrderByCategory(profileId);
    }

    /**
     * Создаёт новый инвариант для профиля.
     *
     * @param profileId идентификатор профиля
     * @param category  категория правила
     * @param ruleText  текст правила
     * @return сохранённая сущность
     */
    @Transactional
    public AgentInvariantEntity addInvariant(String profileId, String category, String ruleText) {
        return repository.save(new AgentInvariantEntity(profileId, category, ruleText));
    }

    /**
     * Обновляет категорию и текст существующего инварианта.
     * Выбрасывает 404 если инвариант не найден.
     *
     * @param id       идентификатор инварианта
     * @param category новая категория
     * @param ruleText новый текст правила
     * @return обновлённая сущность
     */
    @Transactional
    public AgentInvariantEntity updateInvariant(Long id, String category, String ruleText) {
        AgentInvariantEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        entity.update(category, ruleText);
        return repository.save(entity);
    }

    /**
     * Удаляет инвариант по идентификатору.
     * Выбрасывает 404 если инвариант не найден.
     *
     * @param id идентификатор инварианта
     */
    @Transactional
    public void removeInvariant(Long id) {
        if (!repository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        repository.deleteById(id);
    }
}
