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

import java.util.List;
import java.util.Map;

/**
 * Spring-конфигурация MCP-сервера на базе официального Java SDK.
 * Использует Streamable HTTP транспорт (спека 2025-03-26) через один эндпоинт {@code /mcp}.
 * Регистрирует инструменты {@code searchBooks} и {@code getBookDetails} с mock-данными.
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
