package io.book.ai.handler.agent;

import io.book.ai.handler.task.TaskPhase;
import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Объединённый анализатор последнего хода диалога.
 * <p>
 * Выполняет <strong>один</strong> вызов Haiku вместо двух раздельных (FactsExtractor +
 * TaskStateExtractor) и возвращает сразу оба результата: обновлённые факты рабочей
 * памяти и текущее состояние задачи.
 * <p>
 * Принимает последние два сообщения (user + assistant) и текущий блок фактов;
 * полная история не нужна — факты аккумулируются постепенно.
 */
@Component
@RequiredArgsConstructor
public class LastExchangeAnalyzer {

    private static final String HAIKU_MODEL = "claude-haiku-4-5-20251001";

    private static final String SYSTEM_PROMPT = """
            Analyze the conversation exchange and return exactly two sections separated by markers.

            === FACTS ===
            Updated working memory as key: value lines.
            Capture facts in these groups (use these key names where applicable):
            - user_name, user_profession, user_language — who the user is
            - task — what the user is currently working on or asking about
            - goal — the explicit goal or desired outcome
            - constraints — limitations, requirements, non-negotiables
            - decisions — choices the user has made during this session
            - progress — what has been done or resolved so far
            Rules for facts:
            - ALWAYS copy ALL existing facts unchanged unless the new exchange explicitly updates them.
            - Add new facts found in the new exchange.
            - Skip facts that have no value.
            - Return ONLY key: value lines, one per line, no explanations, no headers.

            === TASK ===
            Current task lifecycle phase:
            - PLANNING: the user is scoping, designing, or deciding what to do
            - EXECUTION: the user is actively building or implementing something
            - VALIDATION: the user is testing, reviewing, or debugging
            - DONE: the task is explicitly completed or closed
            - NONE: no clear task is present, or the exchange is purely conversational
            Output exactly 4 lines:
            phase: <PLANNING|EXECUTION|VALIDATION|DONE|NONE>
            step: <one sentence describing what is currently happening, max 10 words, or "none">
            action: <the specific next action to take, max 15 words, or "none">
            plan_approved: <true|false> — true only if the user explicitly confirmed, approved, or accepted a plan in this exchange (e.g. "yes go ahead", "looks good", "approved", "proceed with this plan", "давай", "одобрено", "приступай")
            Rules for task:
            - If phase is NONE, set step and action to "none".
            - Output ONLY these 4 lines in the TASK section.""";

    private final AnthropicClient anthropicClient;

    /**
     * Анализирует последний ход диалога и возвращает обновлённые факты и состояние задачи.
     *
     * @param lastExchange   два последних сообщения: user + assistant
     * @param existingFacts  текущий блок фактов в формате «key: value», либо {@code null}
     * @return результат анализа; поля {@code taskPhase/taskStep/taskAction} могут быть {@code null}
     *         если LLM вернул непarseable вывод для секции TASK
     */
    public AnalysisResult analyze(List<Message> lastExchange, String existingFacts) {
        String conv = lastExchange.stream()
                .map(m -> m.role() + ": " + m.content())
                .collect(Collectors.joining("\n\n"));

        String userContent = existingFacts != null
                ? "Existing facts:\n" + existingFacts + "\n\nNew exchange:\n" + conv
                : "New exchange:\n" + conv;

        AnthropicRequest req = new AnthropicRequest(
                HAIKU_MODEL, 1000,
                SYSTEM_PROMPT,
                null, null,
                List.of(new Message("user", userContent))
        );

        String raw = anthropicClient.callApi(req).text();
        return parse(raw);
    }

    /**
     * Разбирает вывод LLM на секции FACTS и TASK.
     * Если секция TASK не поддаётся парсингу — поля задачи остаются {@code null},
     * факты при этом сохраняются как есть.
     */
    private AnalysisResult parse(String raw) {
        String facts = null;
        TaskPhase phase = null;
        String step = null;
        String action = null;
        boolean planApproved = false;

        int factsStart = raw.indexOf("=== FACTS ===");
        int taskStart  = raw.indexOf("=== TASK ===");

        if (factsStart != -1) {
            int end = taskStart != -1 ? taskStart : raw.length();
            facts = raw.substring(factsStart + "=== FACTS ===".length(), end).trim();
        }

        if (taskStart != -1) {
            String taskSection = raw.substring(taskStart + "=== TASK ===".length()).trim();
            for (String line : taskSection.split("\n")) {
                line = line.trim();
                if (line.startsWith("phase:"))         phase       = parsePhase(line.substring(6).trim());
                else if (line.startsWith("step:"))     step        = line.substring(5).trim();
                else if (line.startsWith("action:"))   action      = line.substring(7).trim();
                else if (line.startsWith("plan_approved:")) planApproved = "true".equalsIgnoreCase(line.substring(13).trim());
            }
        }

        return new AnalysisResult(facts, phase, step, action, planApproved);
    }

    private TaskPhase parsePhase(String value) {
        try {
            return TaskPhase.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Результат анализа одного хода диалога.
     *
     * @param facts        обновлённый блок рабочей памяти в формате «key: value»; может быть {@code null}
     * @param taskPhase    определённая фаза задачи; {@code null} если парсинг не удался
     * @param taskStep     текущий шаг задачи; {@code null} если парсинг не удался
     * @param taskAction   ожидаемое следующее действие; {@code null} если парсинг не удался
     * @param planApproved {@code true} если пользователь явно подтвердил план в данном обмене
     */
    public record AnalysisResult(String facts, TaskPhase taskPhase, String taskStep, String taskAction, boolean planApproved) {}
}
