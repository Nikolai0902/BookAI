package io.book.ai.api.branch;

import java.time.Instant;

/**
 * Описание ветки диалога, возвращаемое клиенту.
 * <p>
 * Для продолжения диалога в ветке клиент должен отправлять последующие сообщения
 * с {@code sessionId = branchSessionId}. Контекст при этом автоматически собирается
 * из истории корневой сессии до точки ветвления плюс собственных сообщений ветки.
 *
 * @param branchSessionId     UUID ветки; используется как {@code sessionId} в запросах к агенту
 * @param rootSessionId       идентификатор родительской сессии
 * @param checkpointMessageId идентификатор сообщения-точки ветвления в родительской сессии
 * @param label               пользовательское название ветки
 * @param createdAt           время создания ветки
 */
public record BranchInfo(
        String branchSessionId,
        String rootSessionId,
        Long checkpointMessageId,
        String label,
        Instant createdAt
) {}
