package io.book.ai.controller;

import io.book.ai.llm.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST-контроллер для просмотра инструментов, доступных через MCP-сервер.
 * Используется для верификации подключения к MCP.
 */
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpInfoController {

    private final McpClient mcpClient;

    /**
     * Возвращает список инструментов, загруженных от MCP-сервера при старте приложения.
     *
     * @return список описаний инструментов (name, description, inputSchema)
     */
    @GetMapping("/tools")
    public List<McpSchema.Tool> getTools() {
        return mcpClient.getAvailableTools();
    }
}
