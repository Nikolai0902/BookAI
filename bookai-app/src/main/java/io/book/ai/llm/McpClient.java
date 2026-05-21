package io.book.ai.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP-клиент, подключающийся к нескольким MCP-серверам через прямые HTTP JSON-RPC запросы.
 * <p>
 * Использует типы {@link McpSchema.Tool} и {@link McpSchema.JsonSchema} из официального SDK
 * для совместимости с остальным кодом, но сетевые вызовы выполняет через {@link RestClient} —
 * это обходит баг {@code HttpClientStreamableHttpTransport} в SDK 0.18.2.
 * <p>
 * При старте подключается ко всем настроенным серверам, объединяет списки инструментов.
 * Маршрутизация {@code callTool} — по имени инструмента к тому серверу, который его зарегистрировал.
 * Недоступные серверы пропускаются (graceful degradation).
 */
@Slf4j
@Component
public class McpClient {

    @Value("${mcp.server.url:http://localhost:8081}")
    private String mcpBookServerUrl;

    @Value("${mcp.scheduler.url:http://localhost:8082}")
    private String mcpSchedulerUrl;

    private final Map<String, RestClient> restClients = new HashMap<>();
    private final Map<String, String> sessionIds = new HashMap<>();
    private final Map<String, String> toolToServerUrl = new HashMap<>();
    private List<McpSchema.Tool> availableTools = new ArrayList<>();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger requestId = new AtomicInteger(1);

    @PostConstruct
    private void connect() {
        for (String url : List.of(mcpBookServerUrl, mcpSchedulerUrl)) {
            try {
                connectToServer(url);
            } catch (Exception e) {
                log.warn("MCP-сервер недоступен по адресу {}: {}", url, e.getMessage());
            }
        }
        log.info("MCP: итого загружено {} инструментов из {} серверов", availableTools.size(), restClients.size());
    }

    /**
     * Выполняет MCP-рукопожатие и загружает список инструментов с одного сервера.
     */
    private void connectToServer(String serverUrl) throws Exception {
        RestClient client = RestClient.builder()
                .baseUrl(serverUrl + "/mcp")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json, text/event-stream")
                .build();

        // initialize — захватываем Mcp-Session-Id из заголовка ответа
        Map<String, Object> initBody = Map.of(
                "jsonrpc", "2.0",
                "id", requestId.getAndIncrement(),
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", "2025-11-25",
                        "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "bookai-app", "version", "1.0")
                )
        );

        ResponseEntity<String> initResponse = client.post()
                .body(initBody)
                .retrieve()
                .toEntity(String.class);

        String sessionId = initResponse.getHeaders().getFirst("Mcp-Session-Id");

        JsonNode serverInfo = objectMapper.readTree(initResponse.getBody())
                .path("result").path("serverInfo");
        log.info("MCP initialize() [{}] — подключено к: {} v{}",
                serverUrl, serverInfo.path("name").asText(), serverInfo.path("version").asText());

        // listTools
        Map<String, Object> listToolsBody = Map.of(
                "jsonrpc", "2.0",
                "id", requestId.getAndIncrement(),
                "method", "tools/list"
        );

        String listResponse = client.post()
                .header("Mcp-Session-Id", sessionId)
                .body(listToolsBody)
                .retrieve()
                .body(String.class);

        JsonNode toolsArray = objectMapper.readTree(extractJson(listResponse)).path("result").path("tools");
        int count = 0;
        for (JsonNode toolNode : toolsArray) {
            McpSchema.Tool tool = parseTool(toolNode);
            availableTools.add(tool);
            toolToServerUrl.put(tool.name(), serverUrl);
            log.info("  [{}] name: {}, description: {}", serverUrl, tool.name(), tool.description());
            count++;
        }

        restClients.put(serverUrl, client);
        sessionIds.put(serverUrl, sessionId);
        log.info("MCP tools/list() [{}] — загружено {} инструментов", serverUrl, count);
    }

    /**
     * Возвращает объединённый список инструментов со всех подключённых серверов.
     *
     * @return типизированный список {@link McpSchema.Tool}
     */
    public List<McpSchema.Tool> getAvailableTools() {
        return availableTools;
    }

    /**
     * Вызывает MCP-инструмент через прямой HTTP JSON-RPC запрос к нужному серверу.
     * Маршрутизация происходит по имени инструмента — каждый инструмент зарегистрирован
     * за конкретным сервером при загрузке.
     *
     * @param name      имя инструмента
     * @param arguments аргументы вызова
     * @return текстовый результат выполнения инструмента
     */
    public String callTool(String name, Map<String, Object> arguments) {
        String serverUrl = toolToServerUrl.get(name);
        if (serverUrl == null) {
            return "Инструмент '" + name + "' не найден";
        }
        try {
            Map<String, Object> body = Map.of(
                    "jsonrpc", "2.0",
                    "id", requestId.getAndIncrement(),
                    "method", "tools/call",
                    "params", Map.of("name", name, "arguments", arguments)
            );

            String response = restClients.get(serverUrl).post()
                    .header("Mcp-Session-Id", sessionIds.get(serverUrl))
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode content = objectMapper.readTree(extractJson(response)).path("result").path("content");
            StringBuilder result = new StringBuilder();
            for (JsonNode block : content) {
                if ("text".equals(block.path("type").asText())) {
                    result.append(block.path("text").asText());
                }
            }
            return result.toString();
        } catch (Exception e) {
            log.error("Ошибка вызова MCP-инструмента '{}': {}", name, e.getMessage());
            return "Ошибка вызова инструмента: " + e.getMessage();
        }
    }

    /**
     * Извлекает JSON из ответа сервера.
     * Сервер может ответить либо чистым JSON, либо в SSE-формате ({@code data: {...}}).
     */
    private String extractJson(String response) {
        if (response == null) return "{}";
        for (String line : response.split("\n")) {
            if (line.trim().startsWith("data:")) {
                return line.trim().substring(5).trim();
            }
        }
        return response.trim();
    }

    private McpSchema.Tool parseTool(JsonNode toolNode) {
        JsonNode schemaNode = toolNode.path("inputSchema");
        Map<String, Object> properties = schemaNode.has("properties")
                ? objectMapper.convertValue(schemaNode.path("properties"), new TypeReference<>() {})
                : Map.of();
        List<String> required = new ArrayList<>();
        schemaNode.path("required").forEach(r -> required.add(r.asText()));

        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                schemaNode.path("type").asText("object"),
                properties,
                required,
                null, null, null
        );

        return McpSchema.Tool.builder()
                .name(toolNode.path("name").asText())
                .description(toolNode.path("description").asText())
                .inputSchema(inputSchema)
                .build();
    }
}
