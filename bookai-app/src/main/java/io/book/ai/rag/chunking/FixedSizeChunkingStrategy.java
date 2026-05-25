package io.book.ai.rag.chunking;

import io.book.ai.rag.config.RagProperties;
import io.book.ai.rag.document.RawDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Стратегия разбиения документа на чанки фиксированного размера с перекрытием.
 * Параметры из конфига: {@code rag.fixed-chunk-size} символов,
 * шаг {@code fixedChunkSize - fixedChunkOverlap}.
 */
@Component
@RequiredArgsConstructor
public class FixedSizeChunkingStrategy implements ChunkingStrategy {

    private final RagProperties props;

    @Override
    public ChunkingStrategyType type() {
        return ChunkingStrategyType.FIXED_SIZE;
    }

    /**
     * Нарезает текст документа окнами фиксированного размера.
     * Каждый чанк получает метку раздела вида {@code FIXED_N}.
     *
     * @param document исходный документ
     * @return список чанков
     */
    @Override
    public List<Chunk> chunk(RawDocument document) {
        String text = document.fullText();
        int chunkSize = props.getFixedChunkSize();
        int overlap = props.getFixedChunkOverlap();
        int step = Math.max(1, chunkSize - overlap);
        List<Chunk> result = new ArrayList<>();
        int chunkIndex = 0;

        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(start + chunkSize, text.length());
            String slice = text.substring(start, end).strip();
            if (!slice.isBlank()) {
                result.add(new Chunk(
                        UUID.randomUUID().toString(),
                        document.source(),
                        document.title(),
                        "FIXED_" + chunkIndex,
                        chunkIndex,
                        slice,
                        countWords(slice)
                ));
                chunkIndex++;
            }
            if (end == text.length()) break;
        }
        return result;
    }

    private int countWords(String text) {
        if (text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }
}
