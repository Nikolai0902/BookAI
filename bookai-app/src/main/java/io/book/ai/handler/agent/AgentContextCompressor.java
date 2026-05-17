package io.book.ai.handler.agent;

import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import io.book.ai.llm.AnthropicRequest.Message;
import io.book.ai.repository.entity.AgentMessageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Компрессор истории диалога для экономии токенов.
 * <p>
 * Оставляет последние N сообщений «как есть» (N задаётся через
 * {@code agent.context.recent-messages-count}), а более старые сообщения
 * заменяет накопительным саммари, которое инжектируется в системный промпт.
 * <p>
 * Саммари обновляется инкрементально: при каждом вызове суммаризируются
 * только сообщения сверх уже покрытых предыдущим саммари.
 * Это позволяет не пересоздавать саммари с нуля на каждом ходу.
 * <p>
 * Используется стратегией {@link io.book.ai.handler.context.strategy.CompressionStrategy}.
 */
@Component
@RequiredArgsConstructor
public class AgentContextCompressor {

    private final AnthropicClient anthropicClient;

    @Value("${agent.context.recent-messages-count:5}")
    private final int recentMessagesCount;

    /**
     * Сжимает историю диалога: оставляет последние N сообщений как есть,
     * остальные заменяет накопительным саммари.
     * <p>
     * Если история не превышает N сообщений, саммари не нужно — возвращается
     * вся история без изменений ({@code summaryUpdated = false}).
     * Если саммари уже существует и покрывает ровно столько сообщений, сколько нужно,
     * новый LLM-вызов не производится ({@code summaryUpdated = false}).
     *
     * @param allMessages           полная история без саммари-записей
     * @param existingSummaryEntity текущая запись-саммари из БД ({@code null} если нет)
     * @param model                 идентификатор модели для генерации саммари
     * @return результат сжатия: саммари (или {@code null}) + последние N сообщений
     */
    public CompressedContext compress(List<AgentMessageEntity> allMessages,
                                      AgentMessageEntity existingSummaryEntity,
                                      String model) {
        if (allMessages.size() <= recentMessagesCount) {
            return new CompressedContext(null, toMessages(allMessages), 0, false);
        }

        int toSummarize = allMessages.size() - recentMessagesCount;
        List<Message> recentMessages = toMessages(allMessages.subList(toSummarize, allMessages.size()));

        String existingText = existingSummaryEntity != null ? existingSummaryEntity.getContent() : null;
        int existingCoverCount = existingSummaryEntity != null ? existingSummaryEntity.getSummaryCoverCount() : 0;

        if (existingText != null && existingCoverCount == toSummarize) {
            return new CompressedContext(existingText, recentMessages, toSummarize, false);
        }

        List<AgentMessageEntity> newMessages = allMessages.subList(existingCoverCount, toSummarize);
        String summary = generateSummary(newMessages, existingText, model);

        return new CompressedContext(summary, recentMessages, toSummarize, true);
    }

    /**
     * Генерирует текст саммари через LLM.
     * Если передано {@code previousSummary}, просит модель дополнить его новыми сообщениями.
     * Если предыдущего саммари нет, суммаризирует список сообщений с нуля.
     *
     * @param messages        сообщения, которые нужно включить в саммари
     * @param previousSummary предыдущее саммари для инкрементального обновления; {@code null} при первом вызове
     * @param model           идентификатор модели
     * @return текст обновлённого саммари
     */
    private String generateSummary(List<AgentMessageEntity> messages, String previousSummary, String model) {
        StringBuilder conv = new StringBuilder();
        for (AgentMessageEntity m : messages) {
            conv.append(m.getRole()).append(": ").append(m.getContent()).append("\n\n");
        }

        String userContent = previousSummary != null
                ? "Previous summary:\n" + previousSummary + "\n\nNew messages to include:\n" + conv
                : "Summarize this conversation concisely:\n" + conv;

        AnthropicRequest req = new AnthropicRequest(
                model, 600,
                "You are a conversation summarizer. Produce a concise factual summary preserving key context.",
                null, null,
                List.of(new Message("user", userContent))
        );
        return anthropicClient.callApi(req).text();
    }

    private List<Message> toMessages(List<AgentMessageEntity> entities) {
        return entities.stream()
                .map(e -> new Message(e.getRole(), e.getContent()))
                .toList();
    }

    /**
     * Результат сжатия истории диалога.
     *
     * @param summaryText     текст накопительного саммари; {@code null} если история
     *                        короче порога и саммари не нужно
     * @param recentMessages  последние N сообщений, передаваемые в LLM «как есть»
     * @param summarizedCount количество сообщений, охваченных саммари
     * @param summaryUpdated  {@code true} если саммари было пересоздано на этом ходу
     *                        и должно быть сохранено в базе
     */
    public record CompressedContext(
            String summaryText,
            List<Message> recentMessages,
            int summarizedCount,
            boolean summaryUpdated
    ) {}
}
