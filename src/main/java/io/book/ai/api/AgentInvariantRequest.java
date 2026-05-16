package io.book.ai.api;

/**
 * Запрос на создание или обновление инварианта профиля.
 *
 * @param category категория: архитектура, стек, бизнес, технические
 * @param ruleText текст правила — конкретное ограничение для ассистента
 */
public record AgentInvariantRequest(String category, String ruleText) {}
