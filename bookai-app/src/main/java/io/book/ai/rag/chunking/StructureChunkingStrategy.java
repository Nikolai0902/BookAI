package io.book.ai.rag.chunking;

import io.book.ai.rag.config.RagProperties;
import io.book.ai.rag.document.RawDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Структурная стратегия разбиения документа на чанки.
 * Детектирует заголовки разделов (нумерованные для русских нормативных документов
 * и Markdown-заголовки) и создаёт по одному чанку на каждый раздел.
 * Слишком длинные разделы подразбиваются методом фиксированного размера.
 * Для исходного кода {@code .java}, {@code .yml}, {@code .xml} применяется fallback на
 * {@link FixedSizeChunkingStrategy}.
 */
@Component
@RequiredArgsConstructor
public class StructureChunkingStrategy implements ChunkingStrategy {

    private static final Pattern NUMBERED_HEADER =
            Pattern.compile("^\\s*\\d+(\\.(\\d+))*\\s+[А-ЯA-ZЁ].*");
    private static final Pattern MARKDOWN_HEADER =
            Pattern.compile("^#{1,3}\\s+.+");

    private final RagProperties props;
    private final FixedSizeChunkingStrategy fixedSizeStrategy;

    @Override
    public ChunkingStrategyType type() {
        return ChunkingStrategyType.STRUCTURE;
    }

    /**
     * Разбивает документ на чанки по заголовкам разделов.
     * Для файлов с исходным кодом делегирует в {@link FixedSizeChunkingStrategy}.
     *
     * @param document исходный документ
     * @return список чанков по разделам
     */
    @Override
    public List<Chunk> chunk(RawDocument document) {
        String src = document.source().toLowerCase();
        if (src.endsWith(".java") || src.endsWith(".yml") || src.endsWith(".yaml") || src.endsWith(".xml")) {
            return fixedSizeStrategy.chunk(document);
        }

        String[] lines = document.fullText().split("\n");
        List<Chunk> result = new ArrayList<>();
        StringBuilder accumulator = new StringBuilder();
        String currentSection = "Введение";
        int chunkIndex = 0;

        for (String line : lines) {
            if (isHeader(line)) {
                if (!accumulator.isEmpty()) {
                    chunkIndex = flushSection(accumulator.toString(), currentSection,
                            document, chunkIndex, result);
                    accumulator.setLength(0);
                }
                currentSection = line.strip();
            } else {
                accumulator.append(line).append('\n');
            }
        }
        if (!accumulator.isEmpty()) {
            flushSection(accumulator.toString(), currentSection, document, chunkIndex, result);
        }
        return result;
    }

    private boolean isHeader(String line) {
        return NUMBERED_HEADER.matcher(line).matches()
                || MARKDOWN_HEADER.matcher(line).matches();
    }

    private int flushSection(String sectionText, String section,
                              RawDocument doc, int startIndex, List<Chunk> result) {
        String text = sectionText.strip();
        if (text.isBlank()) return startIndex;

        int maxSize = props.getFixedChunkSize() * 2;
        if (text.length() <= maxSize) {
            result.add(buildChunk(text, section, doc, startIndex));
            return startIndex + 1;
        }

        int chunkSize = props.getFixedChunkSize();
        int overlap = props.getFixedChunkOverlap();
        int step = Math.max(1, chunkSize - overlap);
        int idx = startIndex;
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(start + chunkSize, text.length());
            String slice = text.substring(start, end).strip();
            if (!slice.isBlank()) {
                result.add(buildChunk(slice, section, doc, idx++));
            }
            if (end == text.length()) break;
        }
        return idx;
    }

    private Chunk buildChunk(String text, String section, RawDocument doc, int index) {
        int words = text.isBlank() ? 0 : text.trim().split("\\s+").length;
        return new Chunk(UUID.randomUUID().toString(),
                doc.source(), doc.title(), section, index, text, words);
    }
}
