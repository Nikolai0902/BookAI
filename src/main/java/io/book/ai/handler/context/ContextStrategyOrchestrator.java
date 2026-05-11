package io.book.ai.handler.context;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Оркестратор стратегий управления контекстом.
 * <p>
 * При старте приложения Spring автоматически инжектирует все бины,
 * реализующие {@link ContextStrategy}, в список {@code strategies}.
 * Оркестратор находит нужную стратегию по типу и делегирует ей вызов —
 * добавление новой стратегии не требует изменений в этом классе.
 * <p>
 * Используется в {@link io.book.ai.handler.agent.AgentBook} как единственная точка
 * входа для работы с контекстом.
 */
@Component
@RequiredArgsConstructor
public class ContextStrategyOrchestrator {

    private final List<ContextStrategy> strategies;

    /**
     * Формирует контекст для передачи в LLM, делегируя стратегии заданного типа.
     *
     * @param type      тип стратегии
     * @param sessionId идентификатор сессии
     * @param model     идентификатор модели
     * @return подготовленный контекст
     * @throws IllegalArgumentException если стратегия с указанным типом не зарегистрирована
     */
    public ContextResult buildContext(ContextStrategyType type, String sessionId, String model) {
        return resolve(type).buildContext(sessionId, model);
    }

    /**
     * Вызывает хук постобработки стратегии после сохранения ответа ассистента.
     *
     * @param type      тип стратегии
     * @param sessionId идентификатор сессии
     * @param model     идентификатор модели
     * @return результат постобработки (например, обновлённые факты), либо {@code null}
     */
    public String afterLlmResponse(ContextStrategyType type, String sessionId, String model) {
        return resolve(type).afterLlmResponse(sessionId, model);
    }

    private ContextStrategy resolve(ContextStrategyType type) {
        return strategies.stream()
                .filter(s -> s.type() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No strategy for: " + type));
    }
}
