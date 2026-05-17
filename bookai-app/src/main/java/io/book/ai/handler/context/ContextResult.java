package io.book.ai.handler.context;

import io.book.ai.llm.AnthropicRequest.Message;

import java.util.List;

/**
 * Результат подготовки контекста стратегией {@link ContextStrategy}.
 * <p>
 * Содержит всё необходимое для вызова LLM: системный промпт (если стратегия его формирует)
 * и список сообщений, которые будут переданы в API. Поля статистики ({@code recentMessagesCount},
 * {@code summarizedMessagesCount}) используются для формирования ответа клиенту.
 *
 * @param systemPrompt          системный промпт, добавляемый к запросу; {@code null} если не нужен
 * @param messages              сообщения, которые передаются в LLM в текущем ходу
 * @param recentMessagesCount   количество «живых» сообщений в контексте (не суммаризованных)
 * @param summarizedMessagesCount количество сообщений, охваченных суммаризацией (0 для стратегий без саммари)
 */
public record ContextResult(
        String systemPrompt,
        List<Message> messages,
        int recentMessagesCount,
        int summarizedMessagesCount
) {}
