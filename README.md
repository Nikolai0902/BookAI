# BookAI

Учебный проект — мультимодульное Spring Boot + React-приложение для исследования возможностей LLM: чат-агент с памятью, RAG по документации, локальная LLM через Ollama, MCP-серверы для работы с инструментами.

## Стек

- **Backend:** Java 25, Spring Boot 4.0.3, Maven multi-module, H2 (file-based)
- **Frontend:** React 18, TypeScript, Vite, Zustand, Tailwind CSS
- **LLM:** Anthropic Claude (облако), qwen2.5:3b через Ollama (локально)
- **RAG:** Voyage AI / nomic-embed-text (эмбеддинги), JSON-индекс с косинусным поиском
- **MCP:** `io.modelcontextprotocol.sdk:mcp:0.18.2`, Streamable HTTP transport

## Структура

```
BookAI/
├── bookai-app/             ← основное приложение (порт 8080) + React-фронт
├── bookai-mcp-server/      ← MCP-сервер с каталогом книг (порт 8081)
└── bookai-mcp-scheduler/   ← MCP-сервер с напоминаниями (порт 8082)
```

## Что умеет

- **Book** — одноразовые LLM-запросы с разными стратегиями (сравнение моделей, режимы рассуждения).
- **Agent** — stateful диалог с памятью (3-слойная модель: история, факты задачи, профиль пользователя), 4 стратегии управления контекстом, ветвление, MCP-инструменты.
- **RAG Chat** — поиск по проектной документации с цитатами и блоком источников, режим «не знаю» когда контекста нет, поддержка облачной и локальной LLM.
- **Local LLM** — чат с qwen2.5:3b через Ollama с настраиваемыми параметрами (temperature, num_ctx, num_predict и т.д.) прямо из UI.
- **Eval-бенчмарк** — `POST /api/rag/eval/run` гоняет 10 контрольных вопросов и возвращает агрегированную статистику качества.

## Кратко по этапам

| Группа задач | Что сделано |
|---|---|
| Task 1–3 | Базовая структура, REST `/api/book`, первая интеграция с Anthropic |
| Task 5–13 | Агент с памятью, стратегии управления контекстом, ветвление диалога |
| Task 14–18 | 3-слойная модель памяти (короткая, рабочая, долгосрочная) |
| Task 19–20 | MCP-серверы (книги + scheduler) и MCP-клиент с маршрутизацией |
| Task 21–25 | RAG: индексация, поиск, фильтрация, цитаты, чат с историей |
| Task 26–29 | Локальная LLM через Ollama, RAG на локальной модели, оптимизация параметров |
| Task 30 | Приватный сервис — UI с телефона по локальной Wi-Fi, защита от переполнения контекста |

## Как запустить

### Переменные окружения

```bash
ANTHROPIC_API_KEY=sk-ant-...     # обязательно
VOYAGE_API_KEY=...               # опционально, для облачных эмбеддингов
ANTHROPIC_MODEL=claude-sonnet-4-6 # опционально
OLLAMA_MODEL=qwen2.5:3b           # опционально
```

### Локальная LLM (опционально)

Для работы локального режима — установить [Ollama](https://ollama.com) и скачать модели:

```bash
ollama pull qwen2.5:3b
ollama pull nomic-embed-text
```

### Сборка и запуск

```bash
# Сборка (включая фронт)
mvn -pl bookai-app -DskipTests package

# Запуск основного приложения
java -jar bookai-app/target/bookai-app-*.jar
# или
mvn -pl bookai-app spring-boot:run

# MCP-серверы (опционально, для агента с инструментами)
mvn -pl bookai-mcp-server spring-boot:run
mvn -pl bookai-mcp-scheduler spring-boot:run
```

Открыть `http://localhost:8080/` в браузере.

### Доступ с других устройств в Wi-Fi

После старта сервиса — узнать IP ноутбука (`ipconfig` → Wi-Fi → IPv4) и открыть `http://<ip>:8080/` с телефона в той же сети.

## Разработка фронта

```bash
cd bookai-app/frontend
npm install
npm run dev    # Vite dev-сервер на :5173, проксирует /api на :8080
```
