package io.book.ai.mcp.config;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Spring-конфигурация MCP-сервера на базе официального Java SDK.
 * Использует Streamable HTTP транспорт (спека 2025-03-26) через один эндпоинт {@code /mcp}.
 * Регистрирует инструменты {@code searchBooks} и {@code getBookDetails} с данными OpenLibrary.
 */
@Slf4j
@Configuration
public class McpServerConfig {

    /**
     * Создаёт провайдер Streamable HTTP транспорта, обрабатывающий GET и POST на {@code /mcp}.
     * Реализует {@link jakarta.servlet.http.HttpServlet}, регистрируется через {@link ServletRegistrationBean}.
     */
    @Bean
    public HttpServletStreamableServerTransportProvider mcpTransportProvider() {
        return HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint("/mcp")
                .build();
    }

    /**
     * Регистрирует MCP-транспорт как servlet на пути {@code /mcp}.
     * Async-режим обязателен для поддержки SSE-fallback в Streamable HTTP.
     */
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
     * Создаёт и запускает MCP-сервер с двумя инструментами OpenLibrary.
     * Привязывается к {@link HttpServletStreamableServerTransportProvider} для обработки запросов.
     *
     * @param provider Streamable HTTP транспорт
     * @return запущенный {@link McpSyncServer}
     */
    @Bean
    public McpSyncServer mcpSyncServer(HttpServletStreamableServerTransportProvider provider) {
        RestClient openLibrary = RestClient.builder()
                .baseUrl("https://openlibrary.org")
                .build();

        McpServerFeatures.SyncToolSpecification searchBooks = searchBooksTool(openLibrary);
        McpServerFeatures.SyncToolSpecification getBookDetails = getBookDetailsTool(openLibrary);

        return McpServer.sync(provider)
                .serverInfo(new McpSchema.Implementation("bookai-mcp-server", "1.0"))
                .tool(searchBooks.tool(), searchBooks.call())
                .tool(getBookDetails.tool(), getBookDetails.call())
                .build();
    }

    private McpServerFeatures.SyncToolSpecification searchBooksTool(RestClient openLibrary) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("searchBooks")
                .description("Поиск книг в каталоге OpenLibrary по названию, автору или теме")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "query", Map.of("type", "string", "description", "Поисковый запрос"),
                                "limit", Map.of("type", "integer", "description", "Макс. количество результатов (по умолчанию 5)")
                        ),
                        List.of("query"),
                        null, null, null
                ))
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            String query = (String) arguments.get("query");
            int limit = arguments.containsKey("limit")
                    ? ((Number) arguments.get("limit")).intValue()
                    : 5;
            try {
                String result = openLibrary.get()
                        .uri("/search.json?q={q}&limit={l}", query, limit)
                        .retrieve()
                        .body(String.class);
                return new McpSchema.CallToolResult(result, false);
            } catch (Exception e) {
                log.warn("searchBooks error for query '{}': {}", query, e.getMessage());
                return new McpSchema.CallToolResult("Ошибка: " + e.getMessage(), true);
            }
        });
    }

    private McpServerFeatures.SyncToolSpecification getBookDetailsTool(RestClient openLibrary) {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("getBookDetails")
                .description("Получение детальной информации о книге по ISBN из каталога OpenLibrary")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of("isbn", Map.of("type", "string", "description", "ISBN книги (10 или 13 цифр)")),
                        List.of("isbn"),
                        null, null, null
                ))
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            String isbn = (String) arguments.get("isbn");
            try {
                String result = openLibrary.get()
                        .uri("/api/books?bibkeys=ISBN:{isbn}&format=json&jscmd=data", isbn)
                        .retrieve()
                        .body(String.class);
                return new McpSchema.CallToolResult(result, false);
            } catch (Exception e) {
                log.warn("getBookDetails error for isbn '{}': {}", isbn, e.getMessage());
                return new McpSchema.CallToolResult("Ошибка: " + e.getMessage(), true);
            }
        });
    }
}
