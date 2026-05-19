package io.book.ai.handler.agent;

import io.book.ai.api.AgentChatRequest;
import io.book.ai.api.AgentChatResponse;
import io.book.ai.api.MemoryLayersSnapshot;
import io.book.ai.api.TaskStateSnapshot;
import io.book.ai.handler.context.ContextResult;
import io.book.ai.handler.context.ContextStrategyOrchestrator;
import io.book.ai.handler.context.ContextStrategyType;
import io.book.ai.handler.task.AgentTaskStateManager;
import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.llm.AnthropicRequest.ToolDefinition;
import io.book.ai.llm.AnthropicRequest.ToolResultContent;
import io.book.ai.llm.AnthropicResponse;
import io.book.ai.llm.LlmResult;
import io.book.ai.llm.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Основной обработчик одного хода диалогового агента.
 * <p>
 * Оркестрирует полный цикл обработки сообщения:
 * <ol>
 *   <li>Сохраняет сообщение пользователя в базе.</li>
 *   <li>Собирает системный промпт из четырёх слоёв: профиль, память, стратегия контекста, состояние задачи.</li>
 *   <li>Вызывает LLM с подготовленным контекстом, системным промптом и списком MCP-инструментов.</li>
 *   <li>Если модель запрашивает инструменты — выполняет их через MCP и повторяет запрос (agentic loop).</li>
 *   <li>Сохраняет ответ ассистента с токен-статистикой.</li>
 *   <li>Запускает постобработку стратегии и обновляет память и состояние задачи.</li>
 *   <li>Возвращает ответ клиенту со снимками памяти и состояния задачи.</li>
 * </ol>
 * Если {@code sessionId} не передан, генерируется новый UUID — так начинается новая сессия.
 * Стратегия по умолчанию — {@link ContextStrategyType#FULL_HISTORY}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentBook {

    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final int MAX_TOOL_ITERATIONS = 5;

    private final AnthropicClient anthropicClient;
    private final AgentSessionStore sessionStore;
    private final ContextStrategyOrchestrator orchestrator;
    private final AgentMemoryManager memoryManager;
    private final AgentTaskStateManager taskStateManager;
    private final LastExchangeAnalyzer lastExchangeAnalyzer;
    private final AgentInvariantManager invariantManager;
    private final McpClient mcpClient;

    @Value("${anthropic.model}")
    private final String defaultModel;

    @Value("${anthropic.max-tokens}")
    private final int maxTokens;

    /**
     * Обрабатывает входящее сообщение пользователя в рамках сессии.
     * <p>
     * Порядок выполнения: сохранение сообщения → сборка системного промпта → вызов LLM →
     * сохранение ответа → постобработка стратегии → обновление памяти и состояния задачи.
     *
     * @param request запрос с текстом и необязательными {@code sessionId}, {@code model},
     *                {@code profileId}, стратегией контекста и флагом памяти
     * @return ответ агента с текстом, статистикой токенов, снимком памяти и состоянием задачи
     */
    public AgentChatResponse chat(AgentChatRequest request) {
        String sessionId = StringUtils.hasText(request.sessionId()) ? request.sessionId() : UUID.randomUUID().toString();
        String model = StringUtils.hasText(request.model()) ? request.model() : defaultModel;
        String profileId = StringUtils.hasText(request.profileId()) ? request.profileId() : AgentMemoryManager.DEFAULT_PROFILE;
        ContextStrategyType strategy = request.strategy() != null ? request.strategy() : ContextStrategyType.FULL_HISTORY;
        boolean memoryEnabled = !Boolean.FALSE.equals(request.memoryEnabled());

        sessionStore.saveMessage(sessionId, ROLE_USER, request.message());

        ContextResult ctx = orchestrator.buildContext(strategy, sessionId, model);
        String systemPrompt = buildSystemPrompt(sessionId, profileId, memoryEnabled, ctx);
        LlmResult result = callLlm(model, ctx.messages(), sessionId, systemPrompt);

        long lastMessageId = sessionStore.saveAssistantMessage(sessionId, result.text(), result.inputTokens(), result.outputTokens());
        orchestrator.afterLlmResponse(strategy, sessionId, model);

        List<Message> lastExchange = List.of(
                new Message(ROLE_USER, request.message()),
                new Message(ROLE_ASSISTANT, result.text()));

        LastExchangeAnalyzer.AnalysisResult analysis = lastExchangeAnalyzer.analyze(
                lastExchange, sessionStore.getFacts(sessionId));

        MemoryLayersSnapshot memorySnapshot = memoryEnabled
                ? memoryManager.updateMemory(sessionId, profileId, analysis.facts())
                : memoryManager.buildSnapshot(sessionId, profileId);
        taskStateManager.updateTaskState(sessionId, analysis.taskPhase(), analysis.taskStep(), analysis.taskAction(), analysis.planApproved());

        return buildResponse(sessionId, result, strategy, ctx, lastMessageId, memorySnapshot,
                taskStateManager.buildSnapshot(sessionId));
    }

    /**
     * Собирает системный промпт из пяти слоёв в порядке приоритета:
     * <ol>
     *   <li>Профиль пользователя — стиль общения, формат ответа, мягкие ограничения.</li>
     *   <li>Инварианты — жёсткие правила профиля, которые нельзя нарушать.</li>
     *   <li>Слои памяти — рабочая память сессии и долговременная память (только если {@code memoryEnabled}).</li>
     *   <li>Системный промпт стратегии контекста — например, саммари для COMPRESSION.</li>
     *   <li>Состояние задачи — текущая фаза, шаг и ожидаемое действие.</li>
     * </ol>
     * Блоки, вернувшие {@code null}, пропускаются.
     */
    private String buildSystemPrompt(String sessionId, String profileId, boolean memoryEnabled, ContextResult ctx) {
        String profilePrompt    = memoryManager.buildProfileSystemPrompt(profileId);
        String invariantsPrompt = invariantManager.buildInvariantsSystemPrompt(profileId);
        String memoryPrompt     = memoryEnabled ? memoryManager.buildMemoryLayersPrompt(sessionId, profileId) : null;
        String taskStatePrompt  = taskStateManager.buildTaskStatePrompt(sessionId);
        return mergeSystemPrompts(
                mergeSystemPrompts(
                        mergeSystemPrompts(mergeSystemPrompts(profilePrompt, invariantsPrompt), memoryPrompt),
                        ctx.systemPrompt()),
                taskStatePrompt);
    }

    /**
     * Объединяет два блока системного промпта через двойной перенос строки.
     * Если один из блоков пустой или {@code null} — возвращает другой без изменений.
     */
    private static String mergeSystemPrompts(String a, String b) {
        if (StringUtils.hasText(a) && StringUtils.hasText(b)) return a + "\n\n" + b;
        return StringUtils.hasText(a) ? a : b;
    }

    /**
     * Вызывает LLM с поддержкой agentic tool-use loop.
     * <p>
     * Передаёт список MCP-инструментов в запрос. Если модель возвращает {@code tool_use}-блоки,
     * выполняет инструменты через {@link McpClient}, добавляет результаты в историю и повторяет вызов.
     * Цикл продолжается до получения {@code stop_reason == "end_turn"} или достижения лимита итераций.
     * При ошибке API сохраняет сообщение об ошибке, чтобы история диалога оставалась консистентной.
     *
     * @param model        идентификатор модели
     * @param history      исходная история сообщений из стратегии контекста
     * @param sessionId    идентификатор сессии (для сохранения ошибок)
     * @param systemPrompt собранный системный промпт
     * @return финальный результат LLM с суммарной статистикой токенов
     */
    private LlmResult callLlm(String model, List<Message> history, String sessionId, String systemPrompt) {
        List<ToolDefinition> tools = convertTools(mcpClient.getAvailableTools());
        List<Message> workingHistory = new ArrayList<>(history);
        LlmResult accumulated = null;
        long startTime = System.currentTimeMillis();

        try {
            for (int i = 0; i < MAX_TOOL_ITERATIONS; i++) {
                AnthropicRequest request = new AnthropicRequest(
                        model, maxTokens, systemPrompt, null, null, workingHistory,
                        tools.isEmpty() ? null : tools);

                AnthropicResponse response = anthropicClient.callRaw(request);
                AnthropicResponse.Usage usage = response.usage();
                long elapsed = System.currentTimeMillis() - startTime;

                String textBlock = response.content().stream()
                        .filter(c -> "text".equals(c.type()))
                        .findFirst()
                        .map(AnthropicResponse.Content::text)
                        .orElse("");

                LlmResult stepResult = new LlmResult(textBlock, usage.input_tokens(), usage.output_tokens(), elapsed);
                accumulated = accumulated == null ? stepResult : accumulated.add(stepResult);

                if (!"tool_use".equals(response.stop_reason())) {
                    return accumulated;
                }

                List<AnthropicResponse.Content> toolUseBlocks = response.content().stream()
                        .filter(c -> "tool_use".equals(c.type()))
                        .toList();

                workingHistory.add(new Message(ROLE_ASSISTANT, response.content()));

                List<ToolResultContent> toolResults = new ArrayList<>();
                for (AnthropicResponse.Content toolUse : toolUseBlocks) {
                    log.info("MCP tool_use: {} {}", toolUse.name(), toolUse.input());
                    String toolResult = mcpClient.callTool(toolUse.name(), toolUse.input());
                    log.info("MCP tool_result: {}", toolResult);
                    toolResults.add(ToolResultContent.of(toolUse.id(), toolResult));
                }
                workingHistory.add(new Message(ROLE_USER, toolResults));
            }
            return accumulated;
        } catch (HttpClientErrorException e) {
            sessionStore.saveMessage(sessionId, ROLE_ASSISTANT, "[ERROR] " + e.getResponseBodyAsString());
            throw e;
        }
    }

    /**
     * Конвертирует MCP-инструменты в формат Anthropic API.
     *
     * @param mcpTools список инструментов, полученных от MCP-сервера
     * @return список {@link ToolDefinition} для передачи в запрос к Anthropic
     */
    private static List<ToolDefinition> convertTools(List<McpSchema.Tool> mcpTools) {
        return mcpTools.stream()
                .map(t -> new ToolDefinition(t.name(), t.description(), t.inputSchema()))
                .toList();
    }

    /**
     * Формирует итоговый ответ клиенту из результата LLM, статистики сессии,
     * снимка памяти и текущего состояния задачи.
     */
    private AgentChatResponse buildResponse(String sessionId, LlmResult result,
                                             ContextStrategyType strategy, ContextResult ctx,
                                             long lastMessageId, MemoryLayersSnapshot memorySnapshot,
                                             TaskStateSnapshot taskSnapshot) {
        return new AgentChatResponse(
                sessionId,
                result.text(),
                result.inputTokens(),
                result.outputTokens(),
                result.responseTimeMs(),
                sessionStore.getTotalInputTokens(sessionId),
                sessionStore.getTotalOutputTokens(sessionId),
                sessionStore.getMessageCount(sessionId) / 2,
                strategy,
                ctx.recentMessagesCount(),
                ctx.summarizedMessagesCount(),
                lastMessageId,
                memorySnapshot,
                taskSnapshot
        );
    }
}
