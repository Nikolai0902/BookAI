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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MCP-клиент, работающий через прямые HTTP JSON-RPC запросы по протоколу Streamable HTTP.
 * <p>
 * Использует типы {@link McpSchema.Tool} и {@link McpSchema.JsonSchema} из официального SDK
 * для совместимости с остальным кодом, но сетевые вызовы выполняет через {@link RestClient} —
 * это обходит баг {@code HttpClientStreamableHttpTransport} в SDK 0.18.2, из-за которого
 * {@code tools/call} зависает (POST-запрос никогда не отправляется на сервер).
 * <p>
 * При старте выполняет {@code initialize} (захватывает {@code Mcp-Session-Id} из заголовка
 * ответа) и {@code tools/list}. Session ID передаётся во все последующие запросы согласно
 * спецификации Streamable HTTP (2025-03-26).
 * Если MCP-сервер недоступен — приложение стартует без инструментов (graceful degradation).
 */
@Slf4j
@Component
public class McpClient {

    @Value("${mcp.server.url:http://localhost:8081}")
    private String mcpServerUrl;

    private RestClient restClient;
    private String sessionId;
    private List<McpSchema.Tool> availableTools = List.of();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger requestId = new AtomicInteger(1);

    @PostConstruct
    private void connect() {
        restClient = RestClient.builder()
                .baseUrl(mcpServerUrl + "/mcp")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Accept", "application/json, text/event-stream")
                .build();
        try {
            initialize();
            listTools();
        } catch (Exception e) {
            log.warn("MCP-сервер недоступен по адресу {}: {}", mcpServerUrl, e.getMessage());
        }
    }

    /**
     * Выполняет MCP-рукопожатие и захватывает {@code Mcp-Session-Id} из заголовка ответа.
     */
    private void initialize() throws Exception {
        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "id", requestId.getAndIncrement(),
                "method", "initialize",
                "params", Map.of(
                        "protocolVersion", "2025-11-25",
                        "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "bookai-app", "version", "1.0")
                )
        );

        ResponseEntity<String> response = restClient.post()
                .body(body)
                .retrieve()
                .toEntity(String.class);

        sessionId = response.getHeaders().getFirst("Mcp-Session-Id");

        JsonNode serverInfo = objectMapper.readTree(response.getBody())
                .path("result").path("serverInfo");
        log.info("MCP initialize() — подключено к: {} v{}",
                serverInfo.path("name").asText(),
                serverInfo.path("version").asText());
    }

    /**
     * Получает список инструментов и кэширует их в {@code availableTools}.
     */
    private void listTools() throws Exception {
        Map<String, Object> body = Map.of(
                "jsonrpc", "2.0",
                "id", requestId.getAndIncrement(),
                "method", "tools/list"
        );

        String response = restClient.post()
                .header("Mcp-Session-Id", sessionId)
                .body(body)
                .retrieve()
                .body(String.class);

        JsonNode toolsArray = objectMapper.readTree(extractJson(response)).path("result").path("tools");
        List<McpSchema.Tool> tools = new ArrayList<>();
        for (JsonNode toolNode : toolsArray) {
            tools.add(parseTool(toolNode));
        }
        availableTools = tools;

        log.info("MCP tools/list() — загружено {} инструментов:", availableTools.size());
        availableTools.forEach(tool -> {
            log.info("  name        : {}", tool.name());
            log.info("  description : {}", tool.description());
            log.info("  inputSchema : {}", tool.inputSchema());
        });
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

    /**
     * Возвращает список инструментов, полученных от MCP-сервера при старте.
     *
     * @return типизированный список {@link McpSchema.Tool}
     */
    public List<McpSchema.Tool> getAvailableTools() {
        return availableTools;
    }

    /**
     * Вызывает MCP-инструмент через прямой HTTP JSON-RPC запрос.
     * Передаёт {@code Mcp-Session-Id}, полученный при {@code initialize()}.
     *
     * @param name      имя инструмента
     * @param arguments аргументы вызова
     * @return текстовый результат выполнения инструмента
     */
    public String callTool(String name, Map<String, Object> arguments) {
        if (availableTools.isEmpty()) {
            return "MCP-сервер недоступен";
        }
        try {
            Map<String, Object> body = Map.of(
                    "jsonrpc", "2.0",
                    "id", requestId.getAndIncrement(),
                    "method", "tools/call",
                    "params", Map.of("name", name, "arguments", arguments)
            );

            String response = restClient.post()
                    .header("Mcp-Session-Id", sessionId)
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
}
