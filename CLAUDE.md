# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**BookAI** is a Spring Boot 4.0.3 + React application that provides two LLM-powered features:
- **Book assistant** — Single-turn prompt with filtering modes (compare, reasoning strategies)
- **Agent** — Stateful multi-turn dialogue with 4 context management strategies, branching support, and a 3-layer memory model

- **Group ID:** `io.book.ai`
- **Main class:** `io.book.ai.BookAiApplication`
- **Java:** 25, **Build:** Maven
- **DB:** H2 file-based (`./data/bookai`), Hibernate `ddl-auto: create`
- **LLM:** Anthropic API (configurable model, default `claude-sonnet-4-6`)

## Commands

```bash
# Backend
mvn spring-boot:run       # run application
mvn clean package         # build executable JAR
mvn clean compile         # compile only
mvn test                  # run tests

# Frontend
cd frontend
npm install
npm run dev               # dev server
npm run build             # production build
```

## Environment Variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `ANTHROPIC_API_KEY` | **yes** | — | Anthropic API key |
| `ANTHROPIC_MODEL` | no | `claude-sonnet-4-6` | Default LLM model |
| `ANTHROPIC_MAX_TOKENS` | no | `1000` | Default max response tokens |
| `AGENT_RECENT_MESSAGES_COUNT` | no | `5` | Window size for Sliding Window strategy |
| `AGENT_SLIDING_WINDOW_SIZE` | no | `10` | Window size for Sliding Window strategy |
| `AGENT_LONG_TERM_UPDATE_INTERVAL` | no | `7` | How many turns between long-term memory updates |

## Package Structure

```
src/main/java/io/book/ai/
├── BookAiApplication.java
├── api/                         # DTOs (Java records, no Lombok)
│   ├── AgentChatRequest.java    # message, sessionId?, model?, strategy?, memoryEnabled?
│   ├── AgentChatResponse.java   # reply, token stats, lastMessageId, memoryLayersSnapshot
│   ├── MemoryLayersSnapshot.java  # shortTermCount, workingMemory, longTermMemory
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
│   └── InfoController.java      # GET /api/info
├── handler/
│   ├── agent/
│   │   ├── AgentBook.java                    # main dialogue orchestrator
│   │   ├── AgentContextCompressor.java       # incremental LLM summarization
│   │   ├── AgentSessionStore.java            # DB abstraction: messages, facts, branches, long-term memory
│   │   ├── AgentMemoryManager.java           # 3-layer memory orchestrator
│   │   ├── FactsExtractor.java               # Haiku extractor for working memory (key: value)
│   │   └── AgentLongTermMemoryExtractor.java # Haiku extractor for long-term memory from working memory
│   ├── context/
│   │   ├── ContextStrategy.java              # interface: buildContext + afterLlmResponse
│   │   ├── ContextStrategyType.java          # enum: FULL_HISTORY | COMPRESSION | SLIDING_WINDOW | BRANCHING
│   │   ├── ContextResult.java                # record: systemPrompt, messages, recentCount, summarizedCount
│   │   ├── ContextStrategyOrchestrator.java  # Spring auto-collects List<ContextStrategy>
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
│   └── LlmResult.java
├── repository/
│   ├── AgentMessageRepository.java
│   ├── AgentBranchRepository.java
│   ├── AgentSessionFactsRepository.java
│   ├── AgentLongTermMemoryRepository.java    # findByProfileIdOrderByCategory, findByProfileIdAndKey, deleteByProfileId
│   └── entity/
│       ├── AgentMessageEntity.java           # table: agent_messages
│       ├── AgentBranchEntity.java            # table: agent_branches
│       ├── AgentSessionFactsEntity.java      # table: agent_session_facts
│       └── AgentLongTermMemoryEntity.java    # table: agent_long_term_memory, UNIQUE(profile_id, fact_key)
└── exception/
    └── GlobalExceptionHandler.java
```

## API Endpoints

### `POST /api/book`
Single LLM query with optional filter modes.
- Request: `BookRequest` (prompt, temperature?, model?, filter?)
- Response: `BookResponse` (answer, inputTokens, outputTokens, responseTimeMs, costUsd)

### `POST /api/agent/chat`
Stateful dialogue turn. Auto-generates `sessionId` if not provided.
- Request: `AgentChatRequest` (message, sessionId?, model?, strategy?, memoryEnabled?)
- Response: `AgentChatResponse` (sessionId, reply, token stats, strategy, recentMessagesCount, summarizedMessagesCount, lastMessageId, memoryLayersSnapshot)

### `POST /api/agent/branch`
Create a new branch from a checkpoint in an existing session.
- Request: `BranchCreateRequest` (rootSessionId, checkpointMessageId, label)
- Response: `BranchInfo`

### `GET /api/agent/branch/{rootSessionId}`
List all branches for a session.

### `GET /api/memory/longterm`
Returns all long-term memory entries for the default profile. Debug only.

### `DELETE /api/memory/longterm`
Clears all long-term memory for the default profile.

### `GET /api/memory/working/{sessionId}`
Returns working memory (facts) for a session. Debug only.

### `GET /api/info`
Returns server configuration: `{model, maxTokens}`.

## Context Management Strategies

All strategies implement `ContextStrategy` interface. `ContextStrategyOrchestrator` auto-discovers implementations via Spring's `List<ContextStrategy>` injection.

| Strategy | Behaviour |
|---|---|
| `FULL_HISTORY` | All messages sent as-is (default) |
| `COMPRESSION` | Old messages replaced by incremental LLM summary in system prompt; last N kept verbatim |
| `SLIDING_WINDOW` | Only last N messages sent; older messages stored in DB but not transmitted |
| `BRANCHING` | Context = root messages up to checkpoint + branch messages |

`afterLlmResponse()` is called after every turn for post-processing (e.g. COMPRESSION updates its summary). Memory layer updates are handled separately by `AgentMemoryManager`, not by strategies.

## Memory Model (Task 11)

The 3-layer memory model is infrastructure that runs on top of every context strategy. It is not a strategy itself.

### Layers

| Layer | Table | Scope | Content |
|---|---|---|---|
| Short-term | `agent_messages` | Current session | Raw dialogue messages |
| Working | `agent_session_facts` | Current session | `key: value` facts: task, goal, constraints, decisions, progress, user identity |
| Long-term | `agent_long_term_memory` | Cross-session | User profile and explicit decisions: name, profession, preferences |

### Per-turn flow in `AgentBook.chat()`

**Before LLM:**
1. `AgentMemoryManager.buildMemorySystemPrompt(sessionId)` — reads working + long-term memory, injects as system prompt block
2. Strategy's `buildContext()` — builds message list per strategy rules
3. `mergeSystemPrompts()` — concatenates memory block + strategy system prompt

**After LLM:**
1. `AgentSessionStore.saveAssistantMessage()` — persists response
2. `orchestrator.afterLlmResponse()` — strategy post-processing (COMPRESSION only)
3. `AgentMemoryManager.updateMemory(sessionId, lastExchange)`:
   - Every turn: `FactsExtractor` (Haiku) updates working memory from last exchange
   - Every N turns (default 7): `AgentLongTermMemoryExtractor` (Haiku) reads full working memory → extracts cross-session facts → upserts into `agent_long_term_memory`

### Why working memory feeds long-term

Long-term updates run every 7 turns. If the extractor read only the last exchange, facts from turn 1 ("my name is Alex") would be gone by turn 7. Working memory aggregates the whole session turn-by-turn, so it's the correct source.

### Memory enable/disable

`AgentChatRequest.memoryEnabled` (default `true`) — when `false`, `AgentBook` skips `buildMemorySystemPrompt` and `updateMemory`. The snapshot is still returned (read-only). Toggle available in UI sidebar.

## Database Schema

**`agent_messages`**

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | auto-increment |
| `session_id` | VARCHAR | session identifier |
| `role` | VARCHAR | `user` or `assistant` |
| `content` | TEXT | message body |
| `created_at` | TIMESTAMP | |
| `input_tokens` | INT | |
| `output_tokens` | INT | |
| `is_summary` | BOOLEAN | true = summary record (Compression strategy) |
| `summary_cover_count` | INT | how many messages this summary covers |

**`agent_branches`**

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | |
| `branch_session_id` | VARCHAR UNIQUE | |
| `root_session_id` | VARCHAR | |
| `checkpoint_message_id` | BIGINT | |
| `label` | VARCHAR | |
| `created_at` | TIMESTAMP | |

**`agent_session_facts`**

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | |
| `session_id` | VARCHAR UNIQUE | |
| `facts` | TEXT | `key: value` lines |
| `updated_at` | TIMESTAMP | |

**`agent_long_term_memory`**

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | |
| `profile_id` | VARCHAR | default `"default"` (single global profile, no auth) |
| `category` | VARCHAR(50) | `profile` or `decision` |
| `fact_key` | VARCHAR(500) | fact identifier — named `fact_key` to avoid H2 reserved word `key` |
| `fact_value` | TEXT | fact content — named `fact_value` to avoid H2 reserved word `value` |
| `updated_at` | TIMESTAMP | |
| UNIQUE | `(profile_id, fact_key)` | upsert by key, category can change |

## Frontend Structure

```
frontend/src/
├── api/
│   ├── agentApi.ts       # chat, createBranch, listBranches, clearLongTermMemory
│   └── bookApi.ts
├── pages/
│   ├── AgentPage.tsx     # /agent — stateful dialogue UI
│   └── BookPage.tsx      # / — single-turn Book assistant
├── components/
│   ├── Agent/
│   │   ├── ChatHistory.tsx
│   │   ├── ChatMessage.tsx
│   │   ├── StrategySelector.tsx   # 4-mode selector, disabled after first message
│   │   ├── MemoryLayersPanel.tsx  # shows all 3 memory layers, 🗑 button clears long-term
│   │   └── BranchingPanel.tsx
│   ├── Chat/
│   └── Sidebar/
├── store/
│   ├── useAgentStore.ts   # Zustand: messages, sessionId, strategy, memoryEnabled, memoryLayersSnapshot, branches
│   └── useAppStore.ts
└── types/api.ts
```

**Tech:** React 18 + TypeScript + Zustand + Axios + Tailwind CSS

## Lombok Conventions

- Use `@RequiredArgsConstructor` on all `@Component`, `@RestController`, `@Service` classes instead of explicit constructors.
- For fields injected via `@Value`, annotate the field (not the constructor parameter) — `lombok.config` is configured to copy `@Value` to generated constructor parameters.
- Do **not** use Lombok on `record` classes — records already generate their own constructors and accessors.
