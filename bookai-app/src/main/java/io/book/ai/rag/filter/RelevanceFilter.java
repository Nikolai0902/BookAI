package io.book.ai.rag.filter;

import io.book.ai.rag.search.SearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Фильтр релевантности: отсекает чанки с косинусным сходством ниже порога.
 * При threshold = 0 фильтр отключён — возвращаются все переданные чанки.
 */
@Slf4j
@Component
public class RelevanceFilter {

    /**
     * Фильтрует список результатов поиска по пороговому значению сходства.
     *
     * @param results   список результатов поиска
     * @param threshold минимальный допустимый score (0 = без фильтрации)
     * @return результат фильтрации с оставшимися чанками и статистикой
     */
    public FilterResult filter(List<SearchResult> results, float threshold) {
        if (threshold <= 0f) {
            return new FilterResult(results, 0, threshold);
        }
        List<SearchResult> kept = results.stream()
                .filter(r -> r.score() >= threshold)
                .toList();
        int removed = results.size() - kept.size();
        log.info("Relevance filter: threshold={} kept={} removed={}", threshold, kept.size(), removed);
        return new FilterResult(kept, removed, threshold);
    }
}
