package io.book.ai.rag.rewrite;

import io.book.ai.llm.AnthropicClient;
import io.book.ai.llm.AnthropicRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Переформулировщик запросов для улучшения семантического поиска.
 * Использует Claude Haiku для раскрытия аббревиатур и добавления контекста.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryRewriter {

    private static final String HAIKU_MODEL = "claude-haiku-4-5-20251001";

    private static final String SYSTEM_PROMPT =
            "Переформулируй вопрос для семантического поиска по технической документации. " +
            "Раскрой аббревиатуры, сделай неявный контекст явным, добавь синонимы ключевых понятий. " +
            "Верни ТОЛЬКО переформулированный вопрос, без пояснений и кавычек.";

    private final AnthropicClient anthropicClient;

    /**
     * Переформулирует запрос для улучшения качества семантического поиска.
     *
     * @param question исходный вопрос пользователя
     * @return переформулированный запрос
     */
    public String rewrite(String question) {
        log.info("Rewriting query: {}", question);
        AnthropicRequest request = new AnthropicRequest(
                HAIKU_MODEL, 200, SYSTEM_PROMPT,
                null, null,
                List.of(new AnthropicRequest.Message("user", question))
        );
        String rewritten = anthropicClient.callApi(request).text().strip();
        log.info("Rewritten query: {}", rewritten);
        return rewritten;
    }
}
