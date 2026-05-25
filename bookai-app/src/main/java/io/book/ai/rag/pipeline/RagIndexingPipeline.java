package io.book.ai.rag.pipeline;

import io.book.ai.rag.api.ComparisonStats;
import io.book.ai.rag.api.IndexingRequest;
import io.book.ai.rag.api.IndexingResponse;
import io.book.ai.rag.chunking.Chunk;
import io.book.ai.rag.chunking.ChunkingStrategy;
import io.book.ai.rag.chunking.ChunkingStrategyType;
import io.book.ai.rag.config.RagProperties;
import io.book.ai.rag.document.DocumentLoader;
import io.book.ai.rag.document.RawDocument;
import io.book.ai.rag.embedding.EmbeddingBatchService;
import io.book.ai.rag.index.IndexEntry;
import io.book.ai.rag.index.IndexFileFormat;
import io.book.ai.rag.index.IndexWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.Set;

/**
 * Оркестратор полного пайплайна RAG-индексации.
 * Загружает документы → разбивает двумя стратегиями → генерирует эмбеддинги → записывает индексы.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagIndexingPipeline {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            ".txt", ".md", ".java", ".yml", ".yaml", ".xml", ".properties"
    );

    private final List<DocumentLoader> documentLoaders;
    private final List<ChunkingStrategy> chunkingStrategies;
    private final EmbeddingBatchService embeddingBatchService;
    private final IndexWriter indexWriter;
    private final RagProperties props;

    /**
     * Запускает полный пайплайн индексации для обеих стратегий чанкинга.
     *
     * @param request необязательный запрос с переопределением путей к документам
     * @return результат с путями к индексам, статистикой и временем выполнения
     * @throws IOException при ошибках чтения или записи файлов
     */
    public IndexingResponse run(IndexingRequest request) throws IOException {
        long start = System.currentTimeMillis();

        List<String> sourcePaths = (request != null && request.sourcePaths() != null)
                ? request.sourcePaths()
                : props.getSourcePaths();

        log.info("RAG pipeline: CWD={}", Path.of(".").toAbsolutePath());
        log.info("RAG pipeline: source paths ({}): {}", sourcePaths.size(), sourcePaths);

        List<RawDocument> documents = loadDocuments(sourcePaths);
        log.info("RAG pipeline: loaded {} documents", documents.size());
        documents.forEach(d -> log.info("  doc: source={} length={}", d.source(), d.fullText().length()));

        String fixedPath = null;
        String structurePath = null;
        List<Chunk> fixedChunks = null;
        List<Chunk> structureChunks = null;

        for (ChunkingStrategy strategy : chunkingStrategies) {
            List<Chunk> chunks = chunkDocuments(documents, strategy);
            log.info("RAG pipeline: strategy={} chunks={}", strategy.type(), chunks.size());
            List<float[]> embeddings = embeddingBatchService.embedChunks(chunks);
            List<IndexEntry> entries = zipToEntries(chunks, embeddings);

            IndexFileFormat format = new IndexFileFormat(
                    strategy.type(),
                    Instant.now().toString(),
                    entries.size(),
                    entries
            );
            Path path = indexWriter.write(format);

            if (strategy.type() == ChunkingStrategyType.FIXED_SIZE) {
                fixedPath = path.toString();
                fixedChunks = chunks;
            } else {
                structurePath = path.toString();
                structureChunks = chunks;
            }
        }

        ComparisonStats stats = buildComparisonStats(fixedChunks, structureChunks);
        return new IndexingResponse(fixedPath, structurePath, stats, System.currentTimeMillis() - start);
    }

    private List<RawDocument> loadDocuments(List<String> sourcePaths) throws IOException {
        List<RawDocument> documents = new ArrayList<>();
        for (String sp : sourcePaths) {
            Path path = Path.of(sp);
            if (Files.isDirectory(path)) {
                try (var stream = Files.list(path)) {
                    for (Path f : stream.filter(Files::isRegularFile)
                            .filter(this::isSupportedFile).toList()) {
                        documents.add(loadFile(f));
                    }
                }
            } else if (Files.isRegularFile(path)) {
                documents.add(loadFile(path));
            }
        }
        return documents;
    }

    private RawDocument loadFile(Path path) throws IOException {
        for (DocumentLoader loader : documentLoaders) {
            if (loader.supports(path)) {
                return loader.load(path);
            }
        }
        throw new IOException("Нет загрузчика для файла: " + path);
    }

    private boolean isSupportedFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".pdf") || TEXT_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    private List<Chunk> chunkDocuments(List<RawDocument> documents, ChunkingStrategy strategy) {
        List<Chunk> all = new ArrayList<>();
        for (RawDocument doc : documents) {
            all.addAll(strategy.chunk(doc));
        }
        return all;
    }

    private List<IndexEntry> zipToEntries(List<Chunk> chunks, List<float[]> embeddings) {
        List<IndexEntry> entries = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            Chunk c = chunks.get(i);
            entries.add(new IndexEntry(
                    c.chunkId(), c.source(), c.title(), c.section(),
                    c.chunkIndex(), c.text(), c.wordCount(), embeddings.get(i)
            ));
        }
        return entries;
    }

    private ComparisonStats buildComparisonStats(List<Chunk> fixed, List<Chunk> structure) {
        return new ComparisonStats(
                computeStrategyStats(ChunkingStrategyType.FIXED_SIZE, fixed),
                computeStrategyStats(ChunkingStrategyType.STRUCTURE, structure)
        );
    }

    private ComparisonStats.StrategyStats computeStrategyStats(
            ChunkingStrategyType type, List<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return new ComparisonStats.StrategyStats(type, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        IntSummaryStatistics lengthStats = chunks.stream()
                .mapToInt(c -> c.text().length())
                .summaryStatistics();
        IntSummaryStatistics wordStats = chunks.stream()
                .mapToInt(Chunk::wordCount)
                .summaryStatistics();
        long uniqueSources = chunks.stream().map(Chunk::source).distinct().count();
        return new ComparisonStats.StrategyStats(
                type,
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
