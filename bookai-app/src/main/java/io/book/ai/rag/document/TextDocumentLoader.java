package io.book.ai.rag.document;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

/**
 * Загрузчик текстовых файлов: {@code .txt}, {@code .md}, {@code .java}, {@code .yml},
 * {@code .yaml}, {@code .xml}, {@code .properties}.
 * Читает файл в UTF-8.
 */
@Component
public class TextDocumentLoader implements DocumentLoader {

    private static final Set<String> SUPPORTED = Set.of(
            ".txt", ".md", ".java", ".yml", ".yaml", ".xml", ".properties"
    );

    @Override
    public boolean supports(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return SUPPORTED.stream().anyMatch(name::endsWith);
    }

    /**
     * Читает файл как строку в кодировке UTF-8.
     *
     * @param path путь к текстовому файлу
     * @return загруженный документ
     * @throws IOException при ошибке чтения файла
     */
    @Override
    public RawDocument load(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        String title = extractTitle(path);
        return new RawDocument(path.toString(), title, text);
    }

    private String extractTitle(Path path) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
