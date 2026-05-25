package io.book.ai.rag.document;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Загрузчик документов в формате PDF через Apache PDFBox.
 * Извлекает весь текст постранично и возвращает как единый {@link RawDocument}.
 */
@Component
public class PdfDocumentLoader implements DocumentLoader {

    @Override
    public boolean supports(Path path) {
        return path.getFileName().toString().toLowerCase().endsWith(".pdf");
    }

    /**
     * Извлекает текст из PDF-файла.
     *
     * @param path путь к PDF-файлу
     * @return документ с извлечённым текстом
     * @throws IOException при ошибке чтения или парсинга файла
     */
    @Override
    public RawDocument load(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            String title = extractTitle(path);
            return new RawDocument(path.toString(), title, text);
        }
    }

    private String extractTitle(Path path) {
        String filename = path.getFileName().toString();
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
