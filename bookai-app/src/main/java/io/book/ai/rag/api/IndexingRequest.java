package io.book.ai.rag.api;

import java.util.List;

/**
 * Запрос на запуск пайплайна индексации.
 * Все поля опциональны — если не указаны, используются значения из конфигурации.
 *
 * @param sourcePaths переопределение списка путей к документам
 */
public record IndexingRequest(List<String> sourcePaths) {}
