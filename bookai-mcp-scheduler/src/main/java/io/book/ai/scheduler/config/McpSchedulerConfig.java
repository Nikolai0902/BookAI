package io.book.ai.scheduler.config;

import io.book.ai.scheduler.service.ReminderService;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Spring-конфигурация MCP-сервера планировщика.
 * Использует Streamable HTTP транспорт (спека 2025-03-26) через эндпоинт {@code /mcp}.
 * Регистрирует инструменты {@code addReminder} и {@code getSummary}.
 */
@Configuration
@RequiredArgsConstructor
public class McpSchedulerConfig {

    private final ReminderService reminderService;

    /**
     * Создаёт провайдер Streamable HTTP транспорта на {@code /mcp}.
     */
    @Bean
    public HttpServletStreamableServerTransportProvider mcpTransportProvider() {
        return HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint("/mcp")
                .build();
    }

    /**
     * Регистрирует MCP-транспорт как servlet на пути {@code /mcp}.
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
     * Создаёт и запускает MCP-сервер с инструментами планировщика.
     *
     * @param provider Streamable HTTP транспорт
     * @return запущенный {@link McpSyncServer}
     */
    @Bean
    public McpSyncServer mcpSyncServer(HttpServletStreamableServerTransportProvider provider) {
        return McpServer.sync(provider)
                .serverInfo(new McpSchema.Implementation("bookai-mcp-scheduler", "1.0"))
                .tool(addReminderTool().tool(), addReminderTool().call())
                .tool(getSummaryTool().tool(), getSummaryTool().call())
                .build();
    }

    private McpServerFeatures.SyncToolSpecification addReminderTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("addReminder")
                .description("Добавляет напоминание с отложенным срабатыванием. Планировщик автоматически отметит его как выполненное через указанное количество секунд.")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(
                                "text", Map.of("type", "string", "description", "Текст напоминания"),
                                "delaySeconds", Map.of("type", "integer", "description", "Задержка в секундах до срабатывания (по умолчанию 60)")
                        ),
                        List.of("text"),
                        null, null, null
                ))
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) -> {
            String text = (String) arguments.get("text");
            int delaySeconds = arguments.containsKey("delaySeconds")
                    ? ((Number) arguments.get("delaySeconds")).intValue()
                    : 60;
            return new McpSchema.CallToolResult(reminderService.addReminder(text, delaySeconds), false);
        });
    }

    private McpServerFeatures.SyncToolSpecification getSummaryTool() {
        McpSchema.Tool tool = McpSchema.Tool.builder()
                .name("getSummary")
                .description("Возвращает сводку напоминаний: количество ожидающих, количество сработавших и список последних 10 сработавших с временем срабатывания.")
                .inputSchema(new McpSchema.JsonSchema(
                        "object",
                        Map.of(),
                        List.of(),
                        null, null, null
                ))
                .build();

        return new McpServerFeatures.SyncToolSpecification(tool, (exchange, arguments) ->
                new McpSchema.CallToolResult(reminderService.getSummary(), false));
    }
}
