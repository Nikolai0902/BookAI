package io.book.ai.rag.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Ответ RAG-запроса с ответом LLM, ссылками на источники и статистикой фильтрации.
 *
 * @param answer          текстовый ответ модели
 * @param citations       список чанков, использованных как контекст
 * @param inputTokens     количество входящих токенов
 * @param outputTokens    количество сгенерированных токенов
 * @param elapsedMs       время ответа в миллисекундах
 * @param retrievedChunks сколько чанков нашёл поиск (до фильтра)
 * @param filteredChunks  сколько чанков передано в промпт (после фильтра)
 * @param rewrittenQuery  переформулированный запрос (null если rewrite не применялся)
 * @param confident       true — LLM ответил по найденным чанкам; false — контекст пуст, вернули «не знаю»
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RagQueryResponse(
        String answer,
        List<Citation> citations,
        int inputTokens,
        int outputTokens,
        long elapsedMs,
        int retrievedChunks,
        int filteredChunks,
        String rewrittenQuery,
        boolean confident
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
