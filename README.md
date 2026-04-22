# Audit Service (ai-training)

**Secure, immutable audit service for enterprise applications**

## 📋 About

An internal **append-only** audit service for recording and storing company events. All events are stored immutably to ensure compliance, security, and observability. The system guarantees audit trail integrity through a hash chain mechanism.

**Key Features:**
- ✅ **Append-only** — no UPDATE/DELETE via API
- 🔒 **Immutable** — protected by database triggers and JPA configuration
- 🔗 **Hash chain** — SHA-256 for tamper-evidence
- 📦 **Retention** — automatic archival of old events
- 🔍 **Full-text search** — filtering by actor, resource, action, outcome, timestamp

## 🛠️ Technology Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Java | 25.0.2 | Runtime |
| Spring Boot | 4.0.5 | Web framework |
| Spring Data JPA | Latest | ORM, persistence |
| Hibernate | Latest | JPA provider |
| PostgreSQL | 16.13 | Primary database |
| Flyway | 11.14.1 | DB migrations |
| Lombok | Latest | Code generation |
| JUnit 5 | Latest | Testing framework |
| Testcontainers | 2.0.5 | Docker test env |

## 🚀 Quick Start

### Requirements
- Java 25+
- PostgreSQL 16+
- Docker (for tests via Testcontainers)

### Running the Application

```bash
# Start PostgreSQL + application from compose.yaml
docker compose up -d

# Check service health
docker compose ps
```

Application will be available at `http://localhost:8080`

To stop the database:

```bash
docker compose down
```

### API Docs (Swagger/OpenAPI)

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

### Local Dev Cycle (quick)

```bash
docker compose up -d
./mvnw test
docker compose down
```

### Running Tests

```bash
# All tests (requires Docker for Testcontainers)
./mvnw test

# Specific test
./mvnw test -Dtest=RetentionServiceIntegrationTest
```

## 📊 REST API

### Create Event
```
POST /audit-events
Content-Type: application/json

{
  "actor": "user:42",
  "action": "invoice.updated",
  "resource": "invoice:777",
  "outcome": "SUCCESS",
  "context": {"ip": "10.0.0.1"}
}

Response 201 Created:
{
  "id": "uuid",
  "timestamp": "2026-04-22T20:00:00Z",
  "actor": "user:42",
  "eventHash": "abc123...",
  "previousHash": "def456..."
}
```

### Search Events
```
GET /audit-events?actor=user:42&resource=invoice:777&from=2026-04-01&to=2026-04-30&sort=timestamp,desc

Response 200 OK:
{
  "content": [...],
  "totalElements": 42,
  "totalPages": 2,
  "currentPage": 0
}
```

## 🏗️ Architecture

```
com.alec.aitraining/
├── domain/          — JPA entities (AuditEvent, ArchivedAuditEvent, Outcome)
├── dto/             — Request/Response DTOs
├── repository/      — Spring Data JPA repositories
├── service/         — Business logic (AuditEventService, RetentionService)
└── web/             — REST controllers and GlobalExceptionHandler
```

## 🔐 Security and Compliance

- **Append-only design** — only way to add data
- **Database triggers** — prevent UPDATE and DELETE at database level
- **Hash chain** — SHA-256 for tamper detection
- **Retention policy** — archival of old events for optimization (copying, not deletion)
- **No delete endpoints** — API does not provide DELETE methods

## 📝 Documentation

- **[AGENTS.md](AGENTS.md)** — Reference guide for AI agents and developers
- **[HELP.md](HELP.md)** — FAQ and helpful information

## ✅ Project Status

- ✅ Core functionality (append, search, hash chain)
- ✅ Database migrations (Flyway V1, V2)
- ✅ Retention service (event archival)
- ✅ Unit & integration tests
- ⚠️ AuditEventIntegrationTest has issues with nested tests in Maven Surefire (technical issue)