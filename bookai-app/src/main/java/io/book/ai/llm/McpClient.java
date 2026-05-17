package io.book.ai.llm;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP-клиент на базе официального Java SDK ({@code io.modelcontextprotocol.sdk:mcp}).
 * Использует Streamable HTTP транспорт ({@link HttpClientStreamableHttpTransport}).
 * При старте выполняет {@code initialize()} и {@code listTools()},
 * кэширует список инструментов и выводит их в консоль.
 * Если MCP-сервер недоступен — приложение стартует без инструментов (graceful degradation).
 */
@Slf4j
@Component
public class McpClient {

    @Value("${mcp.server.url:http://localhost:8081}")
    private String mcpServerUrl;

    private McpSyncClient syncClient;
    private List<McpSchema.Tool> availableTools = List.of();

    @PostConstruct
    private void connect() {
        try {
            HttpClientStreamableHttpTransport transport =
                    HttpClientStreamableHttpTransport.builder(mcpServerUrl + "/mcp").build();

            syncClient = io.modelcontextprotocol.client.McpClient.sync(transport)
                    .clientInfo(new McpSchema.Implementation("bookai-app", "1.0"))
                    .build();

            // 1. Установить MCP-соединение
            McpSchema.InitializeResult initResult = syncClient.initialize();
            log.info("MCP initialize() — подключено к: {} v{}",
                    initResult.serverInfo().name(),
                    initResult.serverInfo().version());

            // 2. Получить список инструментов
            McpSchema.ListToolsResult toolsResult = syncClient.listTools();
            availableTools = toolsResult.tools();

            log.info("MCP tools/list() — загружено {} инструментов:", availableTools.size());
            availableTools.forEach(tool -> {
                log.info("  name        : {}", tool.name());
                log.info("  description : {}", tool.description());
                log.info("  inputSchema : {}", tool.inputSchema());
            });

        } catch (Exception e) {
            log.warn("MCP-сервер недоступен по адресу {}: {}", mcpServerUrl, e.getMessage());
        }
    }

    /**
     * Возвращает список инструментов, полученных от MCP-сервера при старте.
     *
     * @return типизированный список {@link McpSchema.Tool}
     */
    public List<McpSchema.Tool> getAvailableTools() {
        return availableTools;
    }

    /**
     * Вызывает MCP-инструмент по имени с переданными аргументами.
     * Используется агентом для выполнения tool_use запросов от Claude.
     *
     * @param name      имя инструмента
     * @param arguments аргументы вызова
     * @return текстовый результат выполнения инструмента
     */
    public String callTool(String name, java.util.Map<String, Object> arguments) {
        if (syncClient == null) {
            return "MCP-сервер недоступен";
        }
        McpSchema.CallToolResult result = syncClient.callTool(
                new McpSchema.CallToolRequest(name, arguments));
        return result.content().stream()
                .filter(c -> c instanceof McpSchema.TextContent)
                .map(c -> ((McpSchema.TextContent) c).text())
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    @PreDestroy
    private void disconnect() {
        if (syncClient != null) {
            try {
                syncClient.closeGracefully();
            } catch (Exception e) {
                log.debug("Ошибка при закрытии MCP-клиента: {}", e.getMessage());
            }
        }
    }
}
