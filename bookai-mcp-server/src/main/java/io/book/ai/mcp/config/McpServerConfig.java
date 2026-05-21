package io.book.ai.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Spring-конфигурация MCP-сервера на базе официального Java SDK.
 * Использует Streamable HTTP транспорт (спека 2025-03-26) через один эндпоинт {@code /mcp}.
 * Регистрирует инструменты {@code searchBooks}, {@code getBookDetails} с mock-данными
 * и инструменты пайплайна {@code search}, {@code summarize}, {@code saveToFile}.
 */
@Slf4j
@Configuration
public class McpServerConfig {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<Map<String, Object>> BOOKS = List.of(
            Map.of("id", "1", "title", "Война и мир", "author", "Лев Толстой",
                    "year", 1869, "genre", "Роман", "pages", 1274,
                    "description", "Эпический роман о войне 1812 года и судьбах русских семей."),
            Map.of("id", "2", "title", "Анна Каренина", "author", "Лев Толстой",
                    "year", 1877, "genre", "Роман", "pages", 864,
                    "description", "История трагической любви замужней женщины в высшем свете."),
            Map.of("id", "3", "title", "Преступление и наказание", "author", "Фёдор Достоевский",
                    "year", 1866, "genre", "Роман", "pages", 671,
                    "description", "Психологический роман о студенте, совершившем убийство."),
            Map.of("id", "4", "title", "Братья Карамазовы", "author", "Фёдор Достоевский",
                    "year", 1880, "genre", "Роман", "pages", 1008,
                    "description", "Философский роман о вере, морали и семейных конфликтах."),
            Map.of("id", "5", "title", "Мастер и Маргарита", "author", "Михаил Булгаков",
                    "year", 1967, "genre", "Роман", "pages", 480,
                    "description", "Сатирический роман о визите дьявола в советскую Москву."),
            Map.of("id", "6", "title", "Евгений Онегин", "author", "Александр Пушкин",
                    "year", 1833, "genre", "Роман в стихах", "pages", 224,
                    "description", "Роман в стихах о молодом петербургском денди и его любви."),
            Map.of("id", "7", "title", "Мёртвые души", "author", "Николай Гоголь",
                    "year", 1842, "genre", "Поэма", "pages", 352,
                    "description", "Поэма о похождениях Чичикова, скупающего мёртвые крестьянские души."),
            Map.of("id", "8", "title", "Отцы и дети", "author", "Иван Тургенев",
                    "year", 1862, "genre", "Роман", "pages", 224,
                    "description", "Роман о конфликте поколений и нигилизме."),
            Map.of("id", "9", "title", "Вишнёвый сад", "author", "Антон Чехов",
                    "year", 1904, "genre", "Пьеса", "pages", 96,
                    "description", "Пьеса об упадке дворянства и продаже родового имения."),
            Map.of("id", "10", "title", "Герой нашего времени", "author", "Михаил Лермонтов",
                    "year", 1840, "genre", "Роман", "pages", 208,
                    "description", "Психологический портрет «лишнего человека» — Печорина.")
    );

    @Bean
    public HttpServletStreamableServerTransportProvider mcpTransportProvider() {
        return HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint("/mcp")
                .build();
    }

    @Bean
    public ServletRegistrationBean<HttpServletStreamableServerTransportProvider> mcpServletRegistration(
            HttpServletStreamableServerTransportProvider provider) {
        ServletRegistrationBean<HttpServletStreamableServerTransportProvider> bean =
                new ServletRegistrationBean<>(provider, "/mcp");
        bean.setLoadOnStartup(1);
        bean.setAsyncSupported(true);
        return bean;
    }

    /**
     * Создаёт и запускает MCP-сервер с двумя инструментами на mock-данных.
     *
     * @param provider Streamable HTTP транспорт
     * @return запущенный {@link McpSyncServer}
     */
    @Bean
    public McpSyncServer mcpSyncServer(HttpServletStreamableServerTransportProvider provider) {
        return McpServer.sync(provider)
                .serverInfo(new McpSchema.Implementation("bookai-mcp-server", "1.0"))
                .tool(searchBooksTool().tool(), searchBooksTool().call())
                .tool(getBookDetailsTool().tool(), getBookDetailsTool().call())
                .tool(searchTool().tool(), searchTool().call())
                .tool(summarizeTool().tool(), summarizeTool().call())
                .tool(saveToFileTool().tool(), saveToFileTool().call())
                .tool(listSavedFilesTool().tool(), listSavedFilesTool().call())
                .build();
    }

    private McpServerFeatures.SyncToolSpecification searchBooksTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("searchBooks")
                .description("Поиск книг по названию или автору. Возвращает список совпадений с id, названием, автором и жанром.")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "query", Map.of("type", "string", "description", "Поисковый запрос — название или автор"),
                                "limit", Map.of("type", "integer", "description", "Макс. количество результатов (по умолчанию 5)")
                        ),
                        List.of("query"),
                        null, null, null
                ))
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            String query = ((String) arguments.get("query")).toLowerCase();
            int limit = arguments.containsKey("limit")
                    ? ((Number) arguments.get("limit")).intValue()
                    : 5;

            List<Map<String, Object>> matches = BOOKS.stream()
                    .filter(b -> b.get("title").toString().toLowerCase().contains(query)
                            || b.get("author").toString().toLowerCase().contains(query)
                            || b.get("genre").toString().toLowerCase().contains(query))
                    .limit(limit)
                    .map(b -> Map.of(
                            "id", b.get("id"),
                            "title", b.get("title"),
                            "author", b.get("author"),
                            "year", b.get("year"),
                            "genre", b.get("genre")))
                    .toList();

            try {
                String json = MAPPER.writeValueAsString(Map.of("found", matches.size(), "books", matches));
                log.info("searchBooks '{}' — найдено: {}", query, matches.size());
                return new McpSchema.CallToolResult(json, false);
            } catch (Exception e) {
                return new McpSchema.CallToolResult("Ошибка сериализации: " + e.getMessage(), true);
            }
        });
    }

    /**
     * Возвращает список файлов, сохранённых через {@code saveToFile}, из каталога {@code ./data/}.
     * Используется для подтверждения результата пайплайна перед постановкой напоминания.
     */
    private McpServerFeatures.SyncToolSpecification listSavedFilesTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("listSavedFiles")
                .description("Возвращает список файлов, сохранённых через saveToFile. Используй для проверки результатов перед постановкой напоминания.")
                .inputSchema(new McpSchema.JsonSchema(
                        "object", Map.of(), List.of(), null, null, null
                ))
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            try {
                Path dir = Path.of("./data");
                if (!Files.exists(dir)) {
                    return new McpSchema.CallToolResult(
                            MAPPER.writeValueAsString(Map.of("files", List.of(), "count", 0)), false);
                }
                List<String> files = Files.list(dir)
                        .filter(Files::isRegularFile)
                        .map(p -> p.getFileName().toString())
                        .sorted()
                        .toList();
                String result = MAPPER.writeValueAsString(Map.of("files", files, "count", files.size()));
                log.info("listSavedFiles — найдено файлов: {}", files.size());
                return new McpSchema.CallToolResult(result, false);
            } catch (Exception e) {
                log.error("listSavedFiles — ошибка: {}", e.getMessage());
                return new McpSchema.CallToolResult("Ошибка чтения каталога: " + e.getMessage(), true);
            }
        });
    }

    /**
     * Шаг 1 пайплайна: поиск книг с plain-text выводом для передачи в {@code summarize}.
     * Отличие от {@code searchBooksTool}: возвращает текст, а не JSON —
     * это упрощает передачу результата в следующий инструмент пайплайна.
     */
    private McpServerFeatures.SyncToolSpecification searchTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("search")
                .description("Шаг 1 пайплайна. Ищет книги по запросу и возвращает результат в текстовом формате. Передай результат в summarize для создания резюме.")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "query", Map.of("type", "string", "description", "Поисковый запрос — название, автор или жанр"),
                                "limit", Map.of("type", "integer", "description", "Макс. количество результатов (по умолчанию 5)")
                        ),
                        List.of("query"),
                        null, null, null
                ))
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            String query = ((String) arguments.get("query")).toLowerCase();
            int limit = arguments.containsKey("limit")
                    ? ((Number) arguments.get("limit")).intValue()
                    : 5;

            List<Map<String, Object>> matches = BOOKS.stream()
                    .filter(b -> b.get("title").toString().toLowerCase().contains(query)
                            || b.get("author").toString().toLowerCase().contains(query)
                            || b.get("genre").toString().toLowerCase().contains(query))
                    .limit(limit)
                    .toList();

            if (matches.isEmpty()) {
                return new McpSchema.CallToolResult("Книги по запросу '" + query + "' не найдены.", false);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Найдено ").append(matches.size()).append(" книг по запросу '").append(query).append("':\n");
            for (int i = 0; i < matches.size(); i++) {
                Map<String, Object> b = matches.get(i);
                sb.append("[").append(i + 1).append("] «").append(b.get("title")).append("» — ")
                  .append(b.get("author")).append(", ").append(b.get("year"))
                  .append(", ").append(b.get("genre")).append("\n");
            }

            String result = sb.toString().trim();
            log.info("search '{}' — найдено: {}", query, matches.size());
            return new McpSchema.CallToolResult(result, false);
        });
    }

    /**
     * Шаг 2 пайплайна: форматирует текстовый список книг от {@code search}
     * в связное резюме одним абзацем. Результат готов для передачи в {@code saveToFile}.
     */
    private McpServerFeatures.SyncToolSpecification summarizeTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("summarize")
                .description("Шаг 2 пайплайна. Принимает текст от search и создаёт связное резюме в виде одного абзаца. Передай результат в saveToFile для сохранения.")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "text", Map.of("type", "string", "description", "Текст для резюме — обычно результат search"),
                                "maxLength", Map.of("type", "integer", "description", "Макс. длина резюме в символах (по умолчанию 500)")
                        ),
                        List.of("text"),
                        null, null, null
                ))
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            String text = (String) arguments.get("text");
            int maxLength = arguments.containsKey("maxLength")
                    ? ((Number) arguments.get("maxLength")).intValue()
                    : 500;

            if (text == null || text.isBlank()) {
                return new McpSchema.CallToolResult("Нет данных для резюме.", false);
            }

            String[] lines = text.split("\n");
            List<String> bookLines = Arrays.stream(lines)
                    .filter(l -> l.matches("^\\[\\d+\\].*"))
                    .map(l -> l.replaceFirst("^\\[\\d+\\] ", ""))
                    .toList();

            String summary;
            if (bookLines.isEmpty()) {
                summary = text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
            } else {
                String header = lines[0];
                summary = "Резюме: " + header + " " + String.join("; ", bookLines) + ".";
                if (summary.length() > maxLength) {
                    summary = summary.substring(0, maxLength) + "...";
                }
            }

            log.info("summarize — вход: {} симв., резюме: {} симв.", text.length(), summary.length());
            return new McpSchema.CallToolResult(summary, false);
        });
    }

    /**
     * Шаг 3 пайплайна: сохраняет переданный текст в файл {@code ./data/{filename}}.
     * Создаёт каталог {@code data/} если он не существует.
     * Валидирует имя файла регулярным выражением во избежание path traversal.
     */
    private McpServerFeatures.SyncToolSpecification saveToFileTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("saveToFile")
                .description("Шаг 3 пайплайна. Сохраняет текст в файл ./data/{filename} и возвращает подтверждение с абсолютным путём и размером файла.")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "filename", Map.of("type", "string", "description", "Имя файла — только буквы, цифры, точки, дефисы, подчёркивания"),
                                "content",  Map.of("type", "string", "description", "Текст для сохранения — обычно результат summarize")
                        ),
                        List.of("filename", "content"),
                        null, null, null
                ))
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            String filename = (String) arguments.get("filename");
            String content  = (String) arguments.get("content");

            if (!filename.matches("[a-zA-Z0-9._-]+")) {
                return new McpSchema.CallToolResult(
                        "Недопустимое имя файла. Используйте только буквы, цифры, точки, дефисы и подчёркивания.", true);
            }

            try {
                Path dir = Path.of("./data");
                Files.createDirectories(dir);
                Path filePath = dir.resolve(filename);
                Files.writeString(filePath, content, StandardCharsets.UTF_8);
                long bytes = Files.size(filePath);

                String result = MAPPER.writeValueAsString(Map.of(
                        "filename", filename,
                        "path",     filePath.toAbsolutePath().toString(),
                        "bytes",    bytes,
                        "status",   "saved"
                ));
                log.info("saveToFile '{}' — записано {} байт", filename, bytes);
                return new McpSchema.CallToolResult(result, false);
            } catch (Exception e) {
                log.error("saveToFile '{}' — ошибка: {}", filename, e.getMessage());
                return new McpSchema.CallToolResult("Ошибка записи файла: " + e.getMessage(), true);
            }
        });
    }

    private McpServerFeatures.SyncToolSpecification getBookDetailsTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("getBookDetails")
                .description("Получение полной информации о книге по её id из результатов searchBooks.")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of("id", Map.of("type", "string", "description", "Идентификатор книги из результатов searchBooks")),
                        List.of("id"),
                        null, null, null
                ))
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            String id = (String) arguments.get("id");
            return BOOKS.stream()
                    .filter(b -> b.get("id").equals(id))
                    .findFirst()
                    .map(b -> {
                        try {
                            String json = MAPPER.writeValueAsString(b);
                            log.info("getBookDetails id='{}' — найдено: {}", id, b.get("title"));
                            return new McpSchema.CallToolResult(json, false);
                        } catch (Exception e) {
                            return new McpSchema.CallToolResult("Ошибка сериализации: " + e.getMessage(), true);
                        }
                    })
                    .orElseGet(() -> {
                        log.warn("getBookDetails id='{}' — не найдено", id);
                        return new McpSchema.CallToolResult("Книга с id=" + id + " не найдена", true);
                    });
        });
    }
}
