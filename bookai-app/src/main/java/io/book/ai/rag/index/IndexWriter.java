package io.book.ai.rag.index;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.book.ai.rag.chunking.ChunkingStrategyType;
import io.book.ai.rag.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Записывает JSON-индекс документов на диск в директорию {@code rag.index-dir}.
 * Имя файла формируется как {@code index_<стратегия>.json}.
 */
@Component
@RequiredArgsConstructor
public class IndexWriter {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    private final RagProperties props;

    /**
     * Сериализует {@link IndexFileFormat} в форматированный JSON и сохраняет файл.
     *
     * @param index данные индекса для записи
     * @return путь к созданному файлу
     * @throws IOException при ошибке создания директории или записи файла
     */
    public Path write(IndexFileFormat index) throws IOException {
        Path outDir = Path.of(props.getIndexDir());
        Files.createDirectories(outDir);
        Path outPath = outDir.resolve(buildFilename(index.strategy()));

        MAPPER.writeValue(outPath.toFile(), index);
        return outPath;
    }

    /**
     * Возвращает ожидаемый путь к файлу индекса для заданной стратегии.
     *
     * @param strategy тип стратегии чанкинга
     * @return путь к файлу индекса
     */
    public Path getIndexPath(ChunkingStrategyType strategy) {
        return Path.of(props.getIndexDir()).resolve(buildFilename(strategy));
    }

    private String buildFilename(ChunkingStrategyType strategy) {
        return "index_" + strategy.name().toLowerCase() + ".json";
    }
}
