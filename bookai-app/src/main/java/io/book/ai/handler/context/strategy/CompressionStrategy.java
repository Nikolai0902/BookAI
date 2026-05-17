package io.book.ai.handler.context.strategy;

import io.book.ai.handler.agent.AgentContextCompressor;
import io.book.ai.handler.agent.AgentContextCompressor.CompressedContext;
import io.book.ai.handler.agent.AgentSessionStore;
import io.book.ai.handler.context.ContextResult;
import io.book.ai.handler.context.ContextStrategy;
import io.book.ai.handler.context.ContextStrategyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Стратегия LLM-суммаризации: сохраняет последние N сообщений «как есть»,
 * а более ранние заменяет накопительным саммари, инжектируемым в системный промпт.
 * <p>
 * Делегирует логику сжатия в {@link AgentContextCompressor}, который работает
 * инкрементально: при каждом обновлении суммаризируются только новые сообщения
 * сверх уже покрытых предыдущим саммари. Итоговая запись-саммари хранится в базе
 * с флагом {@code is_summary = true}.
 */
@Component
@RequiredArgsConstructor
public class CompressionStrategy implements ContextStrategy {

    private static final String SUMMARY_PREFIX = "## Conversation History Summary\n";

    private final AgentSessionStore sessionStore;
    private final AgentContextCompressor compressor;

    @Override
    public ContextStrategyType type() {
        return ContextStrategyType.COMPRESSION;
    }

    /**
     * Загружает сырую историю и саммари из базы, сжимает контекст через
     * {@link AgentContextCompressor} и при необходимости сохраняет обновлённое саммари.
     * Если саммари существует, в системный промпт добавляется заголовок
     * {@code ## Conversation History Summary}.
     */
    @Override
    public ContextResult buildContext(String sessionId, String model) {
        var rawHistory = sessionStore.loadRawHistory(sessionId);
        CompressedContext compressed = compressor.compress(rawHistory, sessionStore.getSummary(sessionId), model);

        if (compressed.summaryUpdated()) {
            sessionStore.saveSummary(sessionId, compressed.summaryText(), compressed.summarizedCount());
        }

        String systemPrompt = compressed.summaryText() != null
                ? SUMMARY_PREFIX + compressed.summaryText()
                : null;

        return new ContextResult(systemPrompt, compressed.recentMessages(),
                compressed.recentMessages().size(), compressed.summarizedCount());
    }
}
