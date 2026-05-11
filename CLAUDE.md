# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**BookAI** is a Spring Boot 4.0.3 + React application that provides two LLM-powered features:
- **Book assistant** — Single-turn prompt with filtering modes (compare, reasoning strategies)
- **Agent** — Stateful multi-turn dialogue with 5 context management strategies and branching support

- **Group ID:** `io.book.ai`
- **Main class:** `io.book.ai.BookAiApplication`
- **Java:** 25, **Build:** Maven
- **DB:** H2 file-based (`./data/bookai`), Hibernate `ddl-auto: update`
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
| `AGENT_RECENT_MESSAGES_COUNT` | no | `5` | Recent messages for Sticky Facts strategy |
| `AGENT_SLIDING_WINDOW_SIZE` | no | `10` | Window size for Sliding Window strategy |

## Package Structure

```
src/main/java/io/book/ai/
├── BookAiApplication.java
├── api/                         # DTOs (Java records, no Lombok)
│   ├── AgentChatRequest.java    # message, sessionId?, model?, strategy?
│   ├── AgentChatResponse.java   # full response with token stats, factsSnapshot, lastMessageId
│   ├── BookRequest.java         # prompt, temperature?, model?, filter?
│   ├── BookResponse.java        # answer, tokens, responseTimeMs, costUsd
│   ├── LlmAskResponse.java
│   ├── LlmCompareResponse.java
│   └── branch/
│       ├── BranchCreateRequest.java   # rootSessionId, checkpointMessageId, label
│       └── BranchInfo.java            # branchSessionId, rootSessionId, checkpointMessageId, label, createdAt
├── controller/
│   ├── AgentController.java     # POST /api/agent/chat, POST/GET /api/agent/branch
│   ├── BookController.java      # POST /api/book
│   └── InfoController.java      # GET /api/info
├── handler/
│   ├── agent/
│   │   ├── AgentBook.java              # main dialogue orchestrator
│   │   ├── AgentContextCompressor.java # incremental LLM summarization
│   │   └── AgentSessionStore.java      # DB abstraction for sessions, facts, branches
│   ├── context/
│   │   ├── ContextStrategy.java        # interface: buildContext + afterLlmResponse
│   │   ├── ContextStrategyType.java    # enum: FULL_HISTORY | COMPRESSION | SLIDING_WINDOW | STICKY_FACTS | BRANCHING
│   │   ├── ContextResult.java          # record: systemPrompt, messages, recentCount, summarizedCount
│   │   ├── ContextStrategyOrchestrator.java  # Spring auto-collects List<ContextStrategy>
│   │   └── strategy/
│   │       ├── FullHistoryStrategy.java
│   │       ├── CompressionStrategy.java
│   │       ├── SlidingWindowStrategy.java
│   │       ├── BranchingStrategy.java
│   │       └── sticky/
│   │           ├── StickyFactsStrategy.java
│   │           └── FactsExtractor.java  # always uses claude-haiku for cost savings
│   ├── BookOrchestrator.java    # routes book requests by filter type, calculates cost
│   ├── LlmHandler.java
│   └── ReasoningHandler.java    # applies reasoning strategies (step-by-step, expert-panel, etc.)
├── llm/
│   ├── AnthropicClient.java     # RestClient-based Anthropic API client
│   ├── AnthropicRequest.java
│   ├── AnthropicResponse.java
│   └── LlmResult.java           # text, inputTokens, outputTokens, responseTimeMs
├── repository/
│   ├── AgentMessageRepository.java       # includes findBySessionIdUpToId for branch history
│   ├── AgentBranchRepository.java
│   ├── AgentSessionFactsRepository.java
│   └── entity/
│       ├── AgentMessageEntity.java       # table: agent_messages
│       ├── AgentBranchEntity.java        # table: agent_branches
│       └── AgentSessionFactsEntity.java  # table: agent_session_facts
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
- Request: `AgentChatRequest` (message, sessionId?, model?, strategy?)
- Response: `AgentChatResponse` (sessionId, reply, token stats, strategy, recentMessagesCount, summarizedMessagesCount, factsSnapshot?, lastMessageId)

### `POST /api/agent/branch`
Create a new branch from a checkpoint in an existing session.
- Request: `BranchCreateRequest` (rootSessionId, checkpointMessageId, label)
- Response: `BranchInfo`

### `GET /api/agent/branch/{rootSessionId}`
List all branches for a session.
- Response: `BranchInfo[]`

### `GET /api/info`
Returns server configuration: `{model, maxTokens}`.

## Context Management Strategies

All strategies implement `ContextStrategy` interface. `ContextStrategyOrchestrator` auto-discovers implementations via Spring's `List<ContextStrategy>` injection.

| Strategy | Behaviour |
|---|---|
| `FULL_HISTORY` | All messages sent as-is (default) |
| `COMPRESSION` | Old messages replaced by incremental LLM summary in system prompt; last N kept verbatim |
| `SLIDING_WINDOW` | Only last N messages sent; older messages stored in DB but not transmitted |
| `STICKY_FACTS` | Facts block (`key: value`) + last N messages; facts updated after each turn via `FactsExtractor` (Haiku) |
| `BRANCHING` | Context = root messages up to checkpoint + branch messages |

`afterLlmResponse()` returns `String` (updated facts or null) to avoid an extra DB round-trip in the caller.

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
| `branch_session_id` | VARCHAR UNIQUE | UUID used as sessionId for branch messages |
| `root_session_id` | VARCHAR | parent session |
| `checkpoint_message_id` | BIGINT | last root message ID included in branch context |
| `label` | VARCHAR | user-friendly name |
| `created_at` | TIMESTAMP | |

**`agent_session_facts`**

| Column | Type | Notes |
|---|---|---|
| `id` | BIGINT PK | |
| `session_id` | VARCHAR UNIQUE | |
| `facts` | TEXT | `key: value` lines |
| `updated_at` | TIMESTAMP | |

## Frontend Structure

```
frontend/src/
├── api/
│   ├── agentApi.ts       # chat, createBranch, listBranches
│   └── bookApi.ts
├── pages/
│   ├── AgentPage.tsx     # /agent — stateful dialogue UI
│   └── BookPage.tsx      # / — single-turn Book assistant
├── components/
│   ├── Agent/
│   │   ├── ChatHistory.tsx
│   │   ├── ChatMessage.tsx
│   │   ├── StrategySelector.tsx   # 5-mode selector, disabled after first message
│   │   ├── StickyFactsPanel.tsx   # shows factsSnapshot when strategy=STICKY_FACTS
│   │   └── BranchingPanel.tsx     # create/switch branches
│   ├── Chat/              # Book assistant components
│   └── Sidebar/           # model selector, temperature, stats
├── store/
│   ├── useAgentStore.ts   # Zustand: messages, sessionId, strategy, facts, branches, compression stats
│   └── useAppStore.ts
└── types/api.ts
```

**Tech:** React 18 + TypeScript + Zustand + Axios + Tailwind CSS

## Lombok Conventions

- Use `@RequiredArgsConstructor` on all `@Component`, `@RestController`, `@Service` classes instead of explicit constructors.
- For fields injected via `@Value`, annotate the field (not the constructor parameter) — `lombok.config` is configured to copy `@Value` to generated constructor parameters.
- Do **not** use Lombok on `record` classes — records already generate their own constructors and accessors.
