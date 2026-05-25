package io.book.ai.rag.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.book.ai.rag.api.ComparisonStats;
import io.book.ai.rag.chunking.ChunkingStrategyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.IntSummaryStatistics;
import java.util.List;

/**
 * Сервис чтения существующих JSON-индексов и расчёта статистики сравнения стратегий.
 * Не вызывает эмбеддинг-API — работает только с данными, уже записанными на диск.
 */
@Service
@RequiredArgsConstructor
public class IndexStatsService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final IndexWriter indexWriter;

    /**
     * Читает оба индекса с диска и возвращает сравнительную статистику.
     *
     * @return статистика по стратегиям FIXED_SIZE и STRUCTURE
     * @throws IOException если один из файлов индекса не найден
     */
    public ComparisonStats load() throws IOException {
        IndexFileFormat fixed = readIndex(ChunkingStrategyType.FIXED_SIZE);
        IndexFileFormat structure = readIndex(ChunkingStrategyType.STRUCTURE);
        return new ComparisonStats(computeStats(fixed), computeStats(structure));
    }

    private IndexFileFormat readIndex(ChunkingStrategyType strategy) throws IOException {
        Path path = indexWriter.getIndexPath(strategy);
        if (!Files.exists(path)) {
            throw new IOException(
                    "Индекс не найден: " + path + ". Сначала выполните POST /api/rag/index.");
        }
        return MAPPER.readValue(path.toFile(), IndexFileFormat.class);
    }

    private ComparisonStats.StrategyStats computeStats(IndexFileFormat index) {
        List<IndexEntry> chunks = index.chunks();
        if (chunks.isEmpty()) {
            return new ComparisonStats.StrategyStats(index.strategy(), 0, 0, 0, 0, 0, 0, 0, 0);
        }

        IntSummaryStatistics lengthStats = chunks.stream()
                .mapToInt(e -> e.text().length())
                .summaryStatistics();
        IntSummaryStatistics wordStats = chunks.stream()
                .mapToInt(IndexEntry::wordCount)
                .summaryStatistics();
        long uniqueSources = chunks.stream()
                .map(IndexEntry::source)
                .distinct()
                .count();

        return new ComparisonStats.StrategyStats(
                index.strategy(),
                (int) uniqueSources,
                chunks.size(),
                Math.round(lengthStats.getAverage() * 10.0) / 10.0,
                lengthStats.getMin(),
                lengthStats.getMax(),
                Math.round(wordStats.getAverage() * 10.0) / 10.0,
                wordStats.getMin(),
                wordStats.getMax()
        );
    }
}
