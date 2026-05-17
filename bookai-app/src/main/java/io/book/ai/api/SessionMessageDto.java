package io.book.ai.api;

/**
 * Сообщение сессии для загрузки истории при переключении на старый разговор.
 *
 * @param role         роль отправителя: {@code user} или {@code assistant}
 * @param content      текст сообщения
 * @param inputTokens  входные токены запроса (только у assistant-сообщений)
 * @param outputTokens выходные токены ответа (только у assistant-сообщений)
 * @param messageId    первичный ключ записи; используется как {@code lastMessageId} для ветвления
 */
public record SessionMessageDto(
        String role,
        String content,
        Integer inputTokens,
        Integer outputTokens,
        Long messageId
) {}
