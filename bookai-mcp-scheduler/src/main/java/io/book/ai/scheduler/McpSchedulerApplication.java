package io.book.ai.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Точка входа MCP-сервера планировщика напоминаний.
 * {@link EnableScheduling} активирует фоновый {@code @Scheduled}-обработчик в {@code ReminderService}.
 */
@SpringBootApplication
@EnableScheduling
public class McpSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpSchedulerApplication.class, args);
    }
}
