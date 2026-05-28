package io.book.ai.rag.api;

/**
 * Результат прогона одного контрольного вопроса через RAG.
 *
 * @param questionId      порядковый номер вопроса
 * @param question        текст вопроса
 * @param answer          ответ модели
 * @param hasSources      true если citations непусты (есть источники)
 * @param hasInlineQuotes true если ответ содержит встроенные ссылки [N]
 * @param confident       true если LLM ответил по чанкам; false — "не знаю"
 * @param maxScore        лучший score среди citations (0 если citations пусты)
 * @param filteredChunks  сколько чанков прошло фильтр
 */
public record EvalResult(
        int questionId,
        String question,
        String answer,
        boolean hasSources,
        boolean hasInlineQuotes,
        boolean confident,
        float maxScore,
        int filteredChunks
) {}
