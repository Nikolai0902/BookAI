package io.book.ai.rag.api;

import java.util.List;

/**
 * Агрегированный отчёт по прогону 10 контрольных вопросов.
 *
 * @param results           детальные результаты по каждому вопросу
 * @param totalQuestions    всего вопросов
 * @param withSources       сколько ответов содержат источники (citations непусты)
 * @param withInlineQuotes  сколько ответов содержат встроенные ссылки [N]
 * @param confident         сколько ответов получены при наличии релевантного контекста
 * @param totalElapsedMs    суммарное время LLM-генерации
 * @param avgElapsedMs      среднее время LLM-генерации на вопрос
 * @param totalOutputTokens суммарное количество сгенерированных токенов
 * @param provider          какой LLM-провайдер использовался для отчёта (например "claude-sonnet-4-6" или "qwen2.5:3b (local)")
 */
public record EvalReport(
        List<EvalResult> results,
        int totalQuestions,
        int withSources,
        int withInlineQuotes,
        int confident,
        long totalElapsedMs,
        double avgElapsedMs,
        int totalOutputTokens,
        String provider
) {}
