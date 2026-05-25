package io.book.ai.rag.api;

import java.util.List;

/**
 * Контрольный вопрос для оценки качества RAG-системы.
 *
 * @param id              порядковый номер
 * @param question        текст вопроса
 * @param expectedAnswer  ключевые факты ожидаемого ответа
 * @param expectedSources ожидаемые источники (файл + раздел)
 */
public record EvalQuestion(
        int id,
        String question,
        String expectedAnswer,
        List<String> expectedSources
) {

    /** Список из 10 контрольных вопросов, основанных на CLAUDE.md. */
    public static List<EvalQuestion> defaultQuestions() {
        return List.of(
                new EvalQuestion(1,
                        "Какие порты использует каждый модуль BookAI?",
                        "bookai-app — 8080, bookai-mcp-server — 8081, bookai-mcp-scheduler — 8082",
                        List.of("CLAUDE.md — Module Structure")),
                new EvalQuestion(2,
                        "Как запустить только MCP-сервер книг?",
                        "mvn -pl bookai-mcp-server spring-boot:run",
                        List.of("CLAUDE.md — Commands")),
                new EvalQuestion(3,
                        "Какую базу данных использует bookai-app?",
                        "H2 file-based, путь ./data/bookai, Hibernate ddl-auto: create",
                        List.of("CLAUDE.md — Module Structure")),
                new EvalQuestion(4,
                        "Какие переменные окружения обязательны для запуска?",
                        "ANTHROPIC_API_KEY — единственная обязательная переменная",
                        List.of("CLAUDE.md — Environment Variables")),
                new EvalQuestion(5,
                        "Как называются четыре стратегии управления контекстом?",
                        "FULL_HISTORY, COMPRESSION, SLIDING_WINDOW, BRANCHING",
                        List.of("CLAUDE.md — Context Management Strategies")),
                new EvalQuestion(6,
                        "Какой транспорт использует MCP-сервер?",
                        "Streamable HTTP, класс HttpServletStreamableServerTransportProvider, путь /mcp",
                        List.of("CLAUDE.md — bookai-mcp-server Package Structure")),
                new EvalQuestion(7,
                        "Какие три слоя памяти есть у агента?",
                        "Short-term (agent_messages), Working (agent_session_facts), Long-term (agent_long_term_memory)",
                        List.of("CLAUDE.md — Memory Model")),
                new EvalQuestion(8,
                        "Как часто обновляется долговременная память агента?",
                        "Каждые N ходов, переменная AGENT_LONG_TERM_UPDATE_INTERVAL, значение по умолчанию 3",
                        List.of("CLAUDE.md — Environment Variables")),
                new EvalQuestion(9,
                        "Какой Java-класс является главным в bookai-app?",
                        "io.book.ai.BookAiApplication",
                        List.of("CLAUDE.md — Module Structure")),
                new EvalQuestion(10,
                        "Что хранится в таблице agent_session_facts?",
                        "id, session_id (UNIQUE), facts (TEXT), updated_at — хранит key:value факты рабочей памяти",
                        List.of("CLAUDE.md — Database Schema"))
        );
    }
}
