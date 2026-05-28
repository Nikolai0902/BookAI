package io.book.ai.rag.api;

import java.util.List;

/**
 * Агрегированный отчёт по прогону 10 контрольных вопросов.
 *
 * @param results          детальные результаты по каждому вопросу
 * @param totalQuestions   всего вопросов
 * @param withSources      сколько ответов содержат источники (citations непусты)
 * @param withInlineQuotes сколько ответов содержат встроенные ссылки [N]
 * @param confident        сколько ответов получены при наличии релевантного контекста
 */
public record EvalReport(
        List<EvalResult> results,
        int totalQuestions,
        int withSources,
        int withInlineQuotes,
        int confident
) {}
