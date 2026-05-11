package io.book.ai.handler.agent;

import io.book.ai.api.branch.BranchInfo;
import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.repository.AgentBranchRepository;
import io.book.ai.repository.AgentMessageRepository;
import io.book.ai.repository.AgentSessionFactsRepository;
import io.book.ai.repository.entity.AgentBranchEntity;
import io.book.ai.repository.entity.AgentMessageEntity;
import io.book.ai.repository.entity.AgentSessionFactsEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Единая точка доступа к данным диалоговых сессий.
 * <p>
 * Инкапсулирует операции с тремя репозиториями:
 * <ul>
 *   <li>{@link AgentMessageRepository} — сообщения и саммари диалога;</li>
 *   <li>{@link AgentSessionFactsRepository} — блок ключевых фактов для стратегии
 *       {@link io.book.ai.handler.context.ContextStrategyType#STICKY_FACTS};</li>
 *   <li>{@link AgentBranchRepository} — метаданные веток для стратегии
 *       {@link io.book.ai.handler.context.ContextStrategyType#BRANCHING}.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class AgentSessionStore {

    private final AgentMessageRepository repository;
    private final AgentSessionFactsRepository factsRepository;
    private final AgentBranchRepository branchRepository;

    /**
     * Загружает историю сессии в виде объектов {@link Message} для передачи в LLM.
     * Записи-саммари (с флагом {@code is_summary = true}) исключаются.
     *
     * @param sessionId идентификатор сессии
     * @return список сообщений в хронологическом порядке
     */
    public List<Message> loadHistory(String sessionId) {
        return repository.findBySessionIdAndSummaryFalseOrderByCreatedAt(sessionId).stream()
                .map(e -> new Message(e.getRole(), e.getContent()))
                .toList();
    }

    /**
     * Загружает сырые сущности сообщений сессии (без саммари-записей).
     * Используется стратегиями, которым нужен доступ к метаданным сообщений
     * (например, {@code AgentContextCompressor} читает {@code summaryCoverCount}).
     *
     * @param sessionId идентификатор сессии
     * @return список сущностей в хронологическом порядке
     */
    public List<AgentMessageEntity> loadRawHistory(String sessionId) {
        return repository.findBySessionIdAndSummaryFalseOrderByCreatedAt(sessionId);
    }

    /**
     * Возвращает актуальную запись-саммари для сессии, если она существует.
     * Саммари генерируется стратегией {@link io.book.ai.handler.context.strategy.CompressionStrategy}.
     *
     * @param sessionId идентификатор сессии
     * @return запись-саммари, либо {@code null} если саммари ещё нет
     */
    public AgentMessageEntity getSummary(String sessionId) {
        return repository.findTopBySessionIdAndSummaryTrueOrderByCreatedAtDesc(sessionId).orElse(null);
    }

    /**
     * Заменяет существующее саммари новым: удаляет старую запись и сохраняет новую.
     * Вызывается из {@link io.book.ai.handler.context.strategy.CompressionStrategy} при обновлении
     * инкрементального саммари.
     *
     * @param sessionId  идентификатор сессии
     * @param content    текст нового саммари
     * @param coverCount количество реальных сообщений, охваченных этим саммари
     */
    @Transactional
    public void saveSummary(String sessionId, String content, int coverCount) {
        repository.deleteBySessionIdAndSummaryTrue(sessionId);
        repository.save(new AgentMessageEntity(sessionId, "summary", content, true, coverCount));
    }

    /**
     * Сохраняет сообщение пользователя или служебное сообщение без токен-статистики.
     *
     * @param sessionId идентификатор сессии
     * @param role      роль отправителя ({@code "user"} или {@code "assistant"})
     * @param content   текст сообщения
     */
    public void saveMessage(String sessionId, String role, String content) {
        repository.save(new AgentMessageEntity(sessionId, role, content));
    }

    /**
     * Сохраняет ответ ассистента вместе с токен-статистикой текущего хода.
     *
     * @param sessionId    идентификатор сессии
     * @param content      текст ответа ассистента
     * @param inputTokens  количество входных токенов запроса
     * @param outputTokens количество выходных токенов ответа
     * @return автогенерированный первичный ключ сохранённой записи; используется как
     *         {@code checkpointMessageId} при создании ветки диалога
     */
    public Long saveAssistantMessage(String sessionId, String content, int inputTokens, int outputTokens) {
        return repository.save(new AgentMessageEntity(sessionId, "assistant", content, inputTokens, outputTokens)).getId();
    }

    /**
     * Возвращает суммарное количество входных токенов по всем ходам сессии.
     *
     * @param sessionId идентификатор сессии
     * @return суммарное число входных токенов; 0 если сессия пуста
     */
    public int getTotalInputTokens(String sessionId) {
        return repository.sumInputTokensBySessionId(sessionId);
    }

    /**
     * Возвращает суммарное количество выходных токенов по всем ходам сессии.
     *
     * @param sessionId идентификатор сессии
     * @return суммарное число выходных токенов; 0 если сессия пуста
     */
    public int getTotalOutputTokens(String sessionId) {
        return repository.sumOutputTokensBySessionId(sessionId);
    }

    /**
     * Возвращает количество реальных сообщений в сессии (без записей-саммари).
     * Используется для расчёта номера хода: {@code count / 2} даёт число завершённых пар
     * «вопрос–ответ».
     *
     * @param sessionId идентификатор сессии
     * @return количество сообщений типа {@code user} и {@code assistant}
     */
    public long getMessageCount(String sessionId) {
        return repository.countBySessionIdAndSummaryFalse(sessionId);
    }

    /**
     * Возвращает текущий блок ключевых фактов для сессии.
     * Используется стратегией {@link io.book.ai.handler.context.strategy.sticky.StickyFactsStrategy}.
     *
     * @param sessionId идентификатор сессии
     * @return строка с фактами в формате «key: value», либо {@code null} если фактов ещё нет
     */
    public String getFacts(String sessionId) {
        return factsRepository.findBySessionId(sessionId)
                .map(AgentSessionFactsEntity::getFacts)
                .orElse(null);
    }

    /**
     * Сохраняет или обновляет блок ключевых фактов для сессии (upsert).
     * Если запись уже существует, обновляется на месте без удаления.
     *
     * @param sessionId идентификатор сессии
     * @param facts     новый блок фактов в формате «key: value»
     */
    @Transactional
    public void saveFacts(String sessionId, String facts) {
        AgentSessionFactsEntity entity = factsRepository.findBySessionId(sessionId)
                .orElseGet(() -> new AgentSessionFactsEntity(sessionId, facts));
        entity.updateFacts(facts);
        factsRepository.save(entity);
    }

    /**
     * Загружает историю с учётом линии предков ветки.
     * <p>
     * Если {@code branchSessionId} является веткой (есть запись в {@code agent_branches}),
     * история собирается из двух частей: сообщения родительской сессии до точки ветвления
     * плюс собственные сообщения ветки. Если это обычная сессия — поведение совпадает
     * с {@link #loadHistory}.
     *
     * @param branchSessionId идентификатор сессии или ветки
     * @return объединённая история в хронологическом порядке
     */
    public List<Message> loadHistoryForBranch(String branchSessionId) {
        return branchRepository.findByBranchSessionId(branchSessionId)
                .map(branch -> {
                    List<AgentMessageEntity> rootPart = repository.findBySessionIdUpToIdOrderByCreatedAt(
                            branch.getRootSessionId(), branch.getCheckpointMessageId());
                    List<AgentMessageEntity> branchPart = repository.findBySessionIdAndSummaryFalseOrderByCreatedAt(branchSessionId);
                    return Stream.concat(rootPart.stream(), branchPart.stream())
                            .map(e -> new Message(e.getRole(), e.getContent()))
                            .toList();
                })
                .orElseGet(() -> loadHistory(branchSessionId));
    }

    /**
     * Создаёт новую ветку диалога от указанной точки в родительской сессии.
     * Генерирует UUID для ветки; дальнейшие сообщения ветки сохраняются под этим UUID
     * в таблице {@code agent_messages}.
     *
     * @param rootSessionId       идентификатор родительской сессии
     * @param checkpointMessageId идентификатор сообщения-точки ветвления
     * @param label               пользовательское название ветки
     * @return описание созданной ветки для возврата клиенту
     */
    @Transactional
    public BranchInfo createBranch(String rootSessionId, Long checkpointMessageId, String label) {
        String branchSessionId = UUID.randomUUID().toString();
        AgentBranchEntity entity = new AgentBranchEntity(branchSessionId, rootSessionId, checkpointMessageId, label);
        branchRepository.save(entity);
        return toBranchInfo(entity);
    }

    /**
     * Возвращает все ветки, созданные из указанной сессии, в хронологическом порядке.
     *
     * @param rootSessionId идентификатор родительской сессии
     * @return список веток; пустой список если веток нет
     */
    public List<BranchInfo> listBranches(String rootSessionId) {
        return branchRepository.findByRootSessionIdOrderByCreatedAt(rootSessionId).stream()
                .map(this::toBranchInfo)
                .toList();
    }

    private BranchInfo toBranchInfo(AgentBranchEntity e) {
        return new BranchInfo(e.getBranchSessionId(), e.getRootSessionId(),
                e.getCheckpointMessageId(), e.getLabel(), e.getCreatedAt());
    }
}
