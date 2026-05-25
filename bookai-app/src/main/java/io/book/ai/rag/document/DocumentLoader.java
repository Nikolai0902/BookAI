package io.book.ai.rag.document;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Стратегия загрузки текстового содержимого из файла заданного формата.
 */
public interface DocumentLoader {

    /**
     * Возвращает {@code true}, если данный загрузчик поддерживает указанный файл.
     *
     * @param path путь к файлу
     * @return {@code true} если файл поддерживается
     */
    boolean supports(Path path);

    /**
     * Загружает текстовое содержимое файла и возвращает как {@link RawDocument}.
     *
     * @param path путь к файлу
     * @return загруженный документ
     * @throws IOException при ошибке чтения файла
     */
    RawDocument load(Path path) throws IOException;
}
