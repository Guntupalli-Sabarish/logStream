# LogStream

A distributed observability platform built with Spring Boot and React. Centralizes logs from multiple services in real-time, with structured querying, distributed tracing, live statistics, service health monitoring, and an alerting engine.

---

## The Problem

When a distributed application fails, developers are blind. Logs are scattered across files and servers, there is no real-time visibility, and debugging means SSH-ing into machines and grepping through files after the fact.

LogStream solves this by providing a single place to ingest, view, query, and alert on logs from all your services — in real time.

---

## How It Works

```
Your Services  ──POST /api/logs──▶  Spring Boot Backend
                                         │
                              ┌──────────┼──────────────┐
                              ▼          ▼               ▼
                         ArrayDeque  BlockingQueue  SseEmitterRegistry
                         (5k ring)   (10k queue)   (push to browser)
                              │          │
                         ReadWriteLock   └──▶ FileStore (JSON Lines)
                                                  logs/system.log
                         AlertEngine ──▶ WebhookNotifier (Slack/HTTP)

Browser ◀──SSE (text/event-stream)──  GET /api/logs/stream
   │
   ├── ⚡ Live Feed   (real-time log list, search, filter)
   ├── 📊 Stats       (rate charts, severity pie, service bar)
   ├── 🔍 Traces      (timeline view by traceId)
   └── 🩺 Health      (per-service status: Healthy / Degraded / Silent)
```

---

## Features

**Real-time streaming** — The dashboard connects via SSE (`EventSource`). Logs are pushed the instant they arrive. No polling, no lag. Browser auto-reconnects on disconnect.

**Structured query API** — Filter logs server-side by severity, service, traceId, regex search, or time range. Max 5,000 entries in memory; O(n) scan is fast enough.

**Distributed tracing** — Logs carry `traceId` and `spanId`. The Trace Explorer renders a full timeline of all logs for a given request across every service.

**Live statistics** — `GET /api/logs/stats` returns severity distribution, top services by volume, rate per minute, and a 10-minute rate history for charts.

**Service health board** — Classifies each service as Healthy, Degraded (has HIGH logs in last 5 min), or Silent (no recent logs).

**Alert engine** — Sliding-window rules: fire when N logs of a given severity appear within X seconds. Alerts delivered to any HTTP webhook (Slack, Discord, or custom). Rules managed via API at runtime.

**Bounded, safe by design** — In-memory store capped at 5,000 logs. Processing queue capped at 10,000. Non-blocking `offer()` on the queue means the API never blocks under load. Graceful shutdown drains the queue before exit.

**JSON Lines on disk** — Every log is persisted as a JSON object on one line. Readable with `jq`, parseable by any tool.

**Java SDK** — A zero-Spring async batching client for shipping logs from any Java 17+ app. Buffers 1,000 entries, flushes every 500 ms via `POST /api/logs/batch`.

---

## Stack

| | |
|---|---|
| Backend | Java 17, Spring Boot 3.2.5 |
| Real-time | Server-Sent Events (`SseEmitter`) |
| Frontend | React 18, Vite |
| Charts | Recharts |
| Disk storage | JSON Lines (`logs/system.log`) |
| Tests | JUnit 5, MockMvc, Mockito |
| Container | Docker (eclipse-temurin:17-jdk-alpine) |

---

## Running Locally

**Prerequisites:** Java 17+, Maven 3.8+, Node.js 18+

```bash
# Terminal 1 — Backend (http://localhost:8080)
mvn spring-boot:run

# Terminal 2 — Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

Logs are written to `logs/system.log` (created automatically, git-ignored).

### Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `ALLOWED_ORIGINS` | `http://localhost:5173` | CORS allow-list |
| `alert.webhook.url` | *(empty)* | Webhook URL for alert delivery |

### Docker

The `Dockerfile` provides a production-ready, multi-stage build. It solves the "it works on my machine" problem by ensuring anyone can run LogStream without needing Java or Maven installed locally.

```bash
docker build -t logstream .

docker run -p 8080:8080 \
  -e ALLOWED_ORIGINS=https://your-app.com \
  -e alert.webhook.url=https://hooks.slack.com/services/xxx \
  logstream
```

---

## API Reference

### Logs

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/logs` | Get logs (query params below) |
| `POST` | `/api/logs` | Submit a single log |
| `POST` | `/api/logs/batch` | Submit a batch (used by SDK) |
| `DELETE` | `/api/logs` | Clear memory, queue, and disk file |
| `GET` | `/api/logs/stats` | Stats: counts, rate history |
| `GET` | `/api/logs/stream` | SSE stream (`text/event-stream`) |

**Query parameters for `GET /api/logs`:**

```
?severity=HIGH
?service=payment-service
?traceId=abc-123
?search=NullPointer.*        # plain string or regex
?from=1714000000000          # epoch ms, inclusive
?to=1714003600000            # epoch ms, inclusive
?limit=100                   # default 500, max 500
```

### Alert Rules

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/alerts/rules` | List active rules |
| `POST` | `/api/alerts/rules` | Create a rule |
| `DELETE` | `/api/alerts/rules/{id}` | Delete a rule |

**Example rule — fires if 5+ HIGH logs appear within 60 seconds:**

```json
{
  "severity": "HIGH",
  "threshold": 5,
  "windowSeconds": 60,
  "cooldownMinutes": 10
}
```

### Log schema

```json
{
  "id":          "uuid — set server-side if omitted",
  "data":        "your log message",
  "severity":    "HIGH | WARN | LOW",
  "timestamp":   "set server-side if omitted",
  "threadName":  "set server-side if omitted",
  "service":     "payment-service",
  "environment": "dev | staging | prod  (defaults to dev)",
  "traceId":     "links related logs across services",
  "spanId":      "identifies a specific operation",
  "stackTrace":  "optional stack trace string"
}
```

---

## Java SDK

The `sdk/` folder contains a standalone Java Client Library. 

Instead of forcing developers to write manual HTTP REST calls (`RestTemplate`, `HttpClient`) to send logs to your platform, they can drop this SDK into their Java 17+ app (no Spring required). The SDK handles async batching, buffering up to 1,000 entries and flushing every 500 ms so their application is never slowed down by logging.

```java
LogStreamClient client = LogStreamClient.builder("http://localhost:8080")
    .service("payment-service")
    .build();

// Simple helpers
client.info("Payment processed");
client.warn("Slow response: 2400ms");
client.error("DB timeout", exception);
client.error("DB timeout", exception, traceId);

// Full control via builder
client.log(
    LogEntry.builder("Custom event")
        .high()
        .service("payment-service")
        .traceId("abc-123")
        .spanId("def-456")
        .build()
);

client.close(); // drains buffer, then shuts down
```

The SDK buffers entries locally and flushes to `POST /api/logs/batch` every 500 ms or when the buffer reaches 100 entries. `close()` is safe to call in a shutdown hook.

---

## Tests

```bash
mvn test
```

17 tests, 0 failures:

| Class | Tests | What is verified |
|---|---|---|
| `AppTest` | 1 | Spring context loads |
| `LogControllerTest` | 6 | GET, POST enrichment, DELETE via MockMvc |
| `FileStoreTest` | 5 | JSON Lines write, newline injection, `clearFile()` |
| `LoggerServiceTest` | 5 | addLog, order, 5k eviction, clear, immutable snapshot |

---

## Project Structure

```
loggingSystem/
├── src/main/java/logger/
│   ├── App.java
│   ├── alert/
│   │   ├── AlertController.java      # GET/POST/DELETE /api/alerts/rules
│   │   ├── AlertEngine.java          # Sliding-window evaluator
│   │   ├── AlertRule.java
│   │   └── WebhookNotifier.java      # HTTP POST on alert fire
│   ├── config/
│   │   └── WebConfig.java            # CORS (env-configurable)
│   ├── controller/
│   │   ├── HomeController.java
│   │   └── LogController.java        # All /api/logs/* endpoints + SSE
│   ├── data/
│   │   ├── Datastore.java
│   │   └── FileStore.java            # JSON Lines writer
│   ├── enums/Severity.java
│   ├── model/
│   │   ├── LogQuery.java             # Server-side filter DTO
│   │   └── LogStats.java             # Stats response DTO
│   ├── pojo/Log.java
│   └── service/
│       ├── Logger.java               # Core engine: store + queue + lifecycle
│       ├── SseEmitterRegistry.java   # Manages active SSE connections
│       └── StatsService.java         # Compute stats from snapshot
├── frontend/src/
│   ├── App.jsx                       # Root: SSE connection + tab nav
│   ├── App.css
│   └── components/
│       ├── LiveFeed.jsx              # Log list, add form, search, filter
│       ├── StatsPanel.jsx            # Recharts: line + pie + bar
│       ├── TraceExplorer.jsx         # Trace ID timeline
│       └── ServiceHealth.jsx         # Per-service status board
├── sdk/
│   ├── pom.xml                       # Standalone Maven project
│   └── src/main/java/logger/sdk/
│       ├── LogEntry.java             # Immutable log + fluent builder
│       └── LogStreamClient.java      # Async batching HTTP client
├── logs/                             # Runtime only — git-ignored
├── Dockerfile
└── pom.xml
```

---

## Design Decisions

| Decision | Choice | Reasoning |
|---|---|---|
| Real-time transport | SSE over WebSocket | Log delivery is server → client only; SSE has no extra library, native browser reconnect, no STOMP config |
| Storage format | JSON Lines over plain text | Injection-safe (Jackson escapes control chars); readable with `jq` |
| Query layer | In-memory (max 5k) | O(5k) scan takes microseconds; avoids a database dependency |
| Backpressure | Drop (`offer()`) over block | Keeps API responsive; log data is observability, not a financial record |
| Alert engine | In-process over microservice | No Kafka or RabbitMQ needed; simple method call, zero latency |
| Shutdown | Poison-pill + 5 s drain | No log loss on `SIGTERM`; consumer finishes its queue before the JVM exits |
| SDK flush | HTTP batch over per-log HTTP | One TCP connection per 100 logs instead of per log; low overhead |
