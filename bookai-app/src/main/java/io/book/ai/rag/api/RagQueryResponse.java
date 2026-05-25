package io.book.ai.rag.api;

import java.util.List;

/**
 * Ответ RAG-запроса с ответом LLM и ссылками на источники.
 *
 * @param answer       текстовый ответ модели
 * @param citations    список чанков, использованных как контекст
 * @param inputTokens  количество входящих токенов
 * @param outputTokens количество сгенерированных токенов
 * @param elapsedMs    время ответа в миллисекундах
 */
public record RagQueryResponse(
        String answer,
        List<Citation> citations,
        int inputTokens,
        int outputTokens,
        long elapsedMs
) {

    /**
     * Ссылка на чанк из индекса.
     *
     * @param rank    порядковый номер (1-based)
     * @param source  путь к исходному файлу
     * @param section заголовок раздела
     * @param score   косинусное сходство с запросом
     * @param snippet первые 200 символов текста чанка
     */
    public record Citation(int rank, String source, String section, float score, String snippet) {}
}
