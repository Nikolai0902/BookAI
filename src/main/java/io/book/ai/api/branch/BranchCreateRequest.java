package io.book.ai.api.branch;

/**
 * Запрос на создание новой ветки диалога.
 *
 * @param rootSessionId       идентификатор сессии, от которой создаётся ветка
 * @param checkpointMessageId идентификатор сообщения (поле {@code id} в таблице {@code agent_messages}),
 *                            которое становится последним в унаследованном контексте ветки;
 *                            обычно соответствует {@code turnNumber} последнего хода ассистента
 * @param label               пользовательское название ветки (отображается в UI)
 */
public record BranchCreateRequest(String rootSessionId, Long checkpointMessageId, String label) {}
