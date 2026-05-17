package io.book.ai.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа MCP-сервера BookAI.
 * Запускается на порту 8081, предоставляет инструменты поиска книг через OpenLibrary API.
 */
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }
}
