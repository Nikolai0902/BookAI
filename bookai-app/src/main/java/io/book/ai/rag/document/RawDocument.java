package io.book.ai.rag.document;

/**
 * Загруженный документ до разбиения на чанки.
 *
 * @param source   путь к исходному файлу
 * @param title    название документа (имя файла без расширения)
 * @param fullText полный текст документа
 */
public record RawDocument(String source, String title, String fullText) {}
