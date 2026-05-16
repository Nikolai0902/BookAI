package io.book.ai.api;

import java.time.Instant;

/**
 * Краткое описание сессии для отображения в истории последних разговоров.
 *
 * @param sessionId    идентификатор сессии
 * @param lastMessage  превью последнего сообщения (обрезается до 80 символов)
 * @param lastMessageAt время последнего сообщения
 * @param turnCount    количество завершённых ходов (пар user + assistant)
 */
public record SessionSummary(
        String sessionId,
        String lastMessage,
        Instant lastMessageAt,
        int turnCount
) {}
