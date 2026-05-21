# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**BookAI** is a Maven multi-module Spring Boot 4.0.3 + React application with two LLM-powered features and two MCP servers:
- **Book assistant** — Single-turn prompt with filtering modes (compare, reasoning strategies)
- **Agent** — Stateful multi-turn dialogue with 4 context management strategies, branching support, and a 3-layer memory model
- **MCP books server** — Standalone MCP server exposing mock book catalogue tools via Streamable HTTP transport
- **MCP scheduler server** — Standalone MCP server with reminder scheduling, background task processor, and aggregated summary tool

## Module Structure

```
BookAI/
├── pom.xml                      ← parent POM (packaging=pom, inherits spring-boot-starter-parent 4.0.3)
├── bookai-app/                  ← main application (port 8080)
│   ├── pom.xml
│   ├── src/
│   ├── frontend/
│   ├── data/                    ← H2 file-based database
│   └── lombok.config
├── bookai-mcp-server/           ← MCP books server (port 8081)
│   ├── pom.xml
│   └── src/
└── bookai-mcp-scheduler/        ← MCP scheduler server (port 8082)
    ├── pom.xml
    └── src/
```

- **Group ID:** `io.book.ai`
- **Main class (app):** `io.book.ai.BookAiApplication`
- **Main class (mcp-server):** `io.book.ai.mcp.McpServerApplication`
- **Main class (mcp-scheduler):** `io.book.ai.scheduler.McpSchedulerApplication`
- **Java:** 25, **Build:** Maven
- **DB (app):** H2 file-based (`./data/bookai` relative to `bookai-app/`), Hibernate `ddl-auto: create`
- **DB (scheduler):** H2 file-based (`./data/scheduler` relative to `bookai-mcp-scheduler/`), Hibernate `ddl-auto: create`
- **LLM:** Anthropic API (configurable model, default `claude-sonnet-4-6`)

## Commands

```bash
# Run modules individually (from project root)
mvn -pl bookai-mcp-server spring-boot:run      # MCP books server on :8081
mvn -pl bookai-mcp-scheduler spring-boot:run   # MCP scheduler server on :8082
mvn -pl bookai-app spring-boot:run             # main app on :8080

# Build
mvn clean package                              # build all modules
mvn -pl bookai-app clean compile               # compile bookai-app only
mvn -pl bookai-mcp-server clean compile        # compile bookai-mcp-server only
mvn -pl bookai-mcp-scheduler clean compile     # compile bookai-mcp-scheduler only

# Frontend (inside bookai-app/)
cd bookai-app/frontend
npm install
npm run dev                                 # dev server
npm run build                               # production build

# If using corporate Maven mirror, add -s settings-local.xml
mvn -pl bookai-app -s settings-local.xml spring-boot:run
```

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `ANTHROPIC_API_KEY` | **yes** | — | Anthropic API key |
| `ANTHROPIC_MODEL` | no | `claude-sonnet-4-6` | Default LLM model |
| `ANTHROPIC_MAX_TOKENS` | no | `1000` | Default max response tokens |
| `AGENT_RECENT_MESSAGES_COUNT` | no | `5` | Window size for Sliding Window strategy |
| `AGENT_SLIDING_WINDOW_SIZE` | no | `10` | Window size for Sliding Window strategy |
| `AGENT_LONG_TERM_UPDATE_INTERVAL` | no | `3` | How many turns between long-term memory updates |
| `MCP_SERVER_URL` | no | `http://localhost:8081` | URL of the MCP books server |
| `MCP_SCHEDULER_URL` | no | `http://localhost:8082` | URL of the MCP scheduler server |

## bookai-app Package Structure

```
bookai-app/src/main/java/io/book/ai/
├── BookAiApplication.java
├── api/                         # DTOs (Java records, no Lombok)
│   ├── AgentChatRequest.java    # message, sessionId?, model?, strategy?, memoryEnabled?
│   ├── AgentChatResponse.java   # reply, token stats, lastMessageId, memoryLayersSnapshot
│   ├── MemoryLayersSnapshot.java
│   ├── BookRequest.java
│   ├── BookResponse.java
│   ├── LlmAskResponse.java
│   ├── LlmCompareResponse.java
│   └── branch/
│       ├── BranchCreateRequest.java
│       └── BranchInfo.java
├── controller/
│   ├── AgentController.java     # POST /api/agent/chat, POST/GET /api/agent/branch
│   ├── MemoryController.java    # GET/DELETE /api/memory/longterm, GET /api/memory/working/{sessionId}
│   ├── BookController.java      # POST /api/book
│   ├── McpInfoController.java   # GET /api/mcp/tools
│   └── InfoController.java      # GET /api/info
├── handler/
│   ├── agent/
│   │   ├── AgentBook.java                    # main dialogue orchestrator
│   │   ├── AgentContextCompressor.java
│   │   ├── AgentSessionStore.java
│   │   ├── AgentMemoryManager.java           # 3-layer memory orchestrator
│   │   ├── FactsExtractor.java
│   │   └── AgentLongTermMemoryExtractor.java
│   ├── context/
│   │   ├── ContextStrategy.java              # interface: buildContext + afterLlmResponse
│   │   ├── ContextStrategyType.java          # enum: FULL_HISTORY | COMPRESSION | SLIDING_WINDOW | BRANCHING
│   │   ├── ContextResult.java
│   │   ├── ContextStrategyOrchestrator.java
│   │   └── strategy/
│   │       ├── FullHistoryStrategy.java
│   │       ├── CompressionStrategy.java
│   │       ├── SlidingWindowStrategy.java
│   │       └── BranchingStrategy.java
│   ├── BookOrchestrator.java
│   ├── LlmHandler.java
│   └── ReasoningHandler.java
├── llm/
│   ├── AnthropicClient.java
│   ├── AnthropicRequest.java
│   ├── AnthropicResponse.java
│   ├── LlmResult.java
│   └── McpClient.java           # Multi-server MCP client: connects to all configured MCP servers, routes callTool() by tool name
├── repository/
│   ├── AgentMessageRepository.java
│   ├── AgentBranchRepository.java
│   ├── AgentSessionFactsRepository.java
│   ├── AgentLongTermMemoryRepository.java
│   └── entity/
│       ├── AgentMessageEntity.java
│       ├── AgentBranchEntity.java
│       ├── AgentSessionFactsEntity.java
│       └── AgentLongTermMemoryEntity.java
└── exception/
    └── GlobalExceptionHandler.java
```

## bookai-mcp-server Package Structure

```
bookai-mcp-server/src/main/java/io/book/ai/mcp/
├── McpServerApplication.java
└── config/
    └── McpServerConfig.java     # HttpServletStreamableServerTransportProvider + McpSyncServer + tools
```

**MCP SDK:** `io.modelcontextprotocol.sdk:mcp:0.18.2`
**Transport:** Streamable HTTP — `HttpServletStreamableServerTransportProvider`, registered as `ServletRegistrationBean` at `/mcp`
**Tools:** `searchBooks(query, limit?)` and `getBookDetails(id)` — mock data (10 Russian classics hardcoded in memory)

## bookai-mcp-scheduler Package Structure

```
bookai-mcp-scheduler/src/main/java/io/book/ai/scheduler/
├── McpSchedulerApplication.java          # @SpringBootApplication + @EnableScheduling
├── config/
│   └── McpSchedulerConfig.java          # HttpServletStreamableServerTransportProvider + McpSyncServer + tools
├── entity/
│   ├── ReminderEntity.java              # id, text, createdAt, fireAt, firedAt, status
│   └── ReminderStatus.java             # PENDING | FIRED
├── repository/
│   └── ReminderRepository.java         # Spring Data JPA
└── service/
    └── ReminderService.java            # addReminder(), getSummary(), @Scheduled processDueReminders()
```

**MCP SDK:** `io.modelcontextprotocol.sdk:mcp:0.18.2`
**Transport:** Streamable HTTP — same pattern as `bookai-mcp-server`
**Tools:** `addReminder(text, delaySeconds?)` and `getSummary()`
**Scheduler:** `@Scheduled(fixedDelay = 5000)` — checks for due PENDING reminders every 5 seconds, transitions them to FIRED

## API Endpoints

### bookai-app (:8080)

| Method | Path | Description |
|---|---|---|
| POST | `/api/book` | Single LLM query |
| POST | `/api/agent/chat` | Stateful dialogue turn |
| POST | `/api/agent/branch` | Create branch from checkpoint |
| GET | `/api/agent/branch/{rootSessionId}` | List branches |
| GET | `/api/memory/longterm` | Long-term memory (debug) |
| DELETE | `/api/memory/longterm` | Clear long-term memory |
| GET | `/api/memory/working/{sessionId}` | Working memory (debug) |
| GET | `/api/mcp/tools` | List MCP tools loaded at startup |
| GET | `/api/info` | Server config: model, maxTokens |

### bookai-mcp-server (:8081)

| Method | Path | Description |
|---|---|---|
| POST | `/mcp` | MCP JSON-RPC endpoint (initialize, tools/list, tools/call) |
| GET | `/mcp` | SSE stream for server-initiated messages (Streamable HTTP fallback) |

### bookai-mcp-scheduler (:8082)

| Method | Path | Description |
|---|---|---|
| POST | `/mcp` | MCP JSON-RPC endpoint (initialize, tools/list, tools/call) |
| GET | `/mcp` | SSE stream for server-initiated messages (Streamable HTTP fallback) |

## MCP Client (McpClient.java)

`McpClient` runs on `@PostConstruct` and connects to **all configured MCP servers** sequentially:
1. For each server URL (`mcp.server.url`, `mcp.scheduler.url`):
   - Direct HTTP POST to `/mcp` with JSON-RPC `initialize` → captures `Mcp-Session-Id` from response header
   - Direct HTTP POST to `/mcp` with JSON-RPC `tools/list` → merges tools into shared `availableTools` list
   - Records which server owns each tool in `Map<String, String> toolToServerUrl`
2. `callTool(name, arguments)` — routes to the correct server by tool name

**Note:** Uses direct `RestClient` HTTP calls (not `McpSyncClient`) because `HttpClientStreamableHttpTransport.callTool()` has a bug in SDK 0.18.2 — it waits for an SSE GET channel that never opens. Types `McpSchema.Tool` and `McpSchema.JsonSchema` from the SDK are still used for compatibility.

Graceful degradation: unavailable servers are skipped at startup; `bookai-app` starts with whatever tools were loaded successfully.

## Context Management Strategies

All strategies implement `ContextStrategy`. `ContextStrategyOrchestrator` auto-discovers via Spring's `List<ContextStrategy>`.

| Strategy | Behaviour |
|---|---|
| `FULL_HISTORY` | All messages sent as-is (default) |
| `COMPRESSION` | Old messages replaced by incremental LLM summary; last N kept verbatim |
| `SLIDING_WINDOW` | Only last N messages sent; older stored in DB but not transmitted |
| `BRANCHING` | Context = root messages up to checkpoint + branch messages |

## Memory Model

3-layer memory runs on top of every context strategy.

| Layer | Table | Scope | Content |
|---|---|---|---|
| Short-term | `agent_messages` | Current session | Raw dialogue messages |
| Working | `agent_session_facts` | Current session | `key: value` facts: task, goal, constraints, decisions, progress |
| Long-term | `agent_long_term_memory` | Cross-session | User profile: name, profession, preferences |

Per-turn flow: `buildMemorySystemPrompt()` → `buildContext()` → LLM → `saveAssistantMessage()` → `afterLlmResponse()` → `updateMemory()`.

## Database Schema

All tables in `bookai-app`. H2 file at `bookai-app/data/bookai`.

**`agent_messages`** — `id, session_id, role, content, created_at, input_tokens, output_tokens, is_summary, summary_cover_count`

**`agent_branches`** — `id, branch_session_id (UNIQUE), root_session_id, checkpoint_message_id, label, created_at`

**`agent_session_facts`** — `id, session_id (UNIQUE), facts (TEXT), updated_at`

**`agent_long_term_memory`** — `id, profile_id, category, fact_key, fact_value, updated_at` — UNIQUE(profile_id, fact_key)

H2 file at `bookai-mcp-scheduler/data/scheduler`.

**`reminders`** — `id, text, created_at, fire_at, fired_at (nullable), status (PENDING|FIRED)`

## Frontend Structure

```
bookai-app/frontend/src/
├── api/
│   ├── agentApi.ts
│   └── bookApi.ts
├── pages/
│   ├── AgentPage.tsx
│   └── BookPage.tsx
├── components/
│   ├── Agent/
│   │   ├── ChatHistory.tsx
│   │   ├── ChatMessage.tsx
│   │   ├── StrategySelector.tsx
│   │   ├── MemoryLayersPanel.tsx
│   │   └── BranchingPanel.tsx
│   ├── Chat/
│   └── Sidebar/
├── store/
│   ├── useAgentStore.ts
│   └── useAppStore.ts
└── types/api.ts
```

**Tech:** React 18 + TypeScript + Zustand + Axios + Tailwind CSS

## Lombok Conventions

- Use `@RequiredArgsConstructor` on all `@Component`, `@RestController`, `@Service` classes.
- For fields injected via `@Value`, annotate the field — `lombok.config` copies `@Value` to constructor parameters.
- Do **not** use Lombok on `record` classes.
- JPA entities: `@Getter @NoArgsConstructor` + explicit constructor + `update()` method. No `@Setter`.

## Record and Nested Class Conventions

- Use Java `record` for all DTOs in `api/` and internal result/snapshot types.
- Helper types used only within one class → nested `public record` inside that class.
- Types used across packages or exposed via API → top-level file in `api/`.

## Javadoc Conventions

- Все Javadoc-комментарии пишутся **на русском языке**.
- Javadoc обязателен для всех `public` классов и `public`/`protected` методов.
- Приватные вспомогательные методы — без Javadoc, если назначение понятно из имени.

## Design Principles

- **SOLID** — одна ответственность; зависимости через интерфейсы; открыт для расширения.
- **DRY** — не дублировать логику; общие компоненты вместо копипасты.
- **KISS** — простое решение предпочтительнее сложного; абстракции только при необходимости.
- **YAGNI** — не реализовывать то, что не нужно прямо сейчас.
