# Audit Service — AGENTS.md

> Справочник для AI-агентов и разработчиков, работающих с этим репозиторием.
> 
> **Последнее обновление:** 22.04.2026 — добавлены Docker Compose, Dockerfile, Swagger/OpenAPI и DevTools

---

## Назначение сервиса

Внутренний **append-only** аудит-сервис. Принимает события от других сервисов компании
и хранит их **иммутабельно** для compliance, security и observability.

Читатели: compliance-офицеры, SRE, security-аналитики.

---

## Технологический стек

| Слой | Технология |
|---|---|
| Runtime | Java 25, Spring Boot 4 |
| Web | Spring MVC (`spring-boot-starter-webmvc`) |
| Persistence | Spring Data JPA + Hibernate, PostgreSQL |
| Migrations | Flyway (`db/migration/V*.sql`) |
| Build | Maven (wrapper: `mvnw.cmd` / `mvnw`) |
| Tests | JUnit 5, Testcontainers (`postgres:16-alpine`), MockMvc |
| API Docs | springdoc OpenAPI + Swagger UI |
| Local infra | Docker Compose (`compose.yaml`) + Dockerfile приложения |
| Utilities | Lombok (`@Slf4j`, `@Builder`, `@Getter`, `@RequiredArgsConstructor`) |

---

## Структура пакетов

```
com.alec.aitraining
├── domain/          — JPA-сущности и enum Outcome
├── dto/             — Record-классы запросов и ответов
├── repository/      — Spring Data JPA репозитории
├── service/         — Бизнес-логика (AuditEventService, RetentionService)
└── web/             — REST-контроллер и GlobalExceptionHandler
```

---

## Жёсткие инварианты — НИКОГДА не нарушать

1. **append-only** — нет `UPDATE`, нет `DELETE` через JPA/приложение.
   - Postgres-триггеры `BEFORE UPDATE` и `BEFORE DELETE` на **обеих** таблицах:
     `audit_events` (V1) и `archived_audit_events` (V3).
   - Все поля сущности `AuditEvent` помечены `updatable = false`.
   - **Retention архивация НЕ должна удалять данные** - это нарушает audit trail.

2. **`timestamp` ставится только сервером** — поле отсутствует в `CreateAuditEventRequest`.
   Значение всегда `Instant.now()` внутри `AuditEventService.append()`.

3. **`actor` обязателен** — `@NotBlank` на DTO, возвращает `400 Bad Request` с `ProblemDetail`.

4. **Hash chain** — каждое событие хранит:
   - `eventHash` — SHA-256 от `timestamp|actor|action|resource|outcome|previousHash`
   - `previousHash` — `eventHash` предыдущего события (null для первого)
   
   Метод `AuditEventService.append()` **`synchronized`** для гарантии консистентности цепочки
   на одном инстансе. При горизонтальном масштабировании заменить на distributed lock
   (например, Postgres advisory lock).

---

## REST API

### `POST /audit-events`
Принимает одно событие. `timestamp` игнорируется, если передан — всегда ставится сервером.

```json
{
  "actor":    "user:42",
  "action":   "invoice.updated",
  "resource": "invoice:777",
  "outcome":  "SUCCESS",
  "context":  { "ip": "10.0.0.1" }
}
```
Ответ: `201 Created` + `AuditEventResponse` (включает `eventHash`, `previousHash`).

### `GET /audit-events`
Поиск с опциональными фильтрами, поддержка пагинации (`page`, `size`, `sort`).

| Параметр | Тип | Описание |
|---|---|---|
| `actor` | string | Точное совпадение |
| `resource` | string | Точное совпадение |
| `action` | string | Точное совпадение |
| `outcome` | `SUCCESS`/`DENIED`/`ERROR` | Enum |
| `from` | ISO-8601 instant | Нижняя граница timestamp |
| `to` | ISO-8601 instant | Верхняя граница timestamp |

`sort` должен быть в формате Spring Data: `property,(asc|desc)`, например
`sort=timestamp,desc` (а не JSON-массив).

### OpenAPI / Swagger
- `GET /v3/api-docs` — OpenAPI JSON
- `GET /swagger-ui.html` — Swagger UI

---

## Retention Policy

- Настраивается через `application.properties`:
  ```properties
  audit.retention.days=90          # после скольких дней события копируются в архив
  audit.retention.cron=0 0 2 * * * # когда запускать job (по умолчанию 02:00 UTC)
  ```
- `RetentionService` батчами (500 записей) копирует устаревшие события
  в `archived_audit_events`. **НЕ удаляет из горячей таблицы** - это нарушает 
  audit trail и compliance требования. Данные остаются доступны для расследований.

---

## Flyway-миграции

| Версия | Файл | Что делает |
|---|---|---|
| V1 | `V1__create_audit_events.sql` | Горячая таблица + DB-триггеры иммутабельности |
| V2 | `V2__create_archived_audit_events.sql` | Архивная таблица |
| V3 | `V3__protect_archived_audit_events.sql` | DB-триггеры иммутабельности на архивной таблице |

**Правило:** новые изменения схемы — только через новый `V{n}__*.sql`.
Никогда не редактировать существующие миграции.

**Зависимость** (для PostgreSQL 16+, Spring Boot BOM управляет версией):
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-database-postgresql</artifactId>
</dependency>
```

---

## Запуск и разработка

```bash
# Сборка и компиляция
./mvnw clean compile

# Запуск всех тестов (требует Docker для Testcontainers)
./mvnw test

# Локальный запуск приложения (требует локальный Postgres)
./mvnw spring-boot:run
```

Для локальной БД используется Docker Compose:
```bash
docker compose up -d
docker compose ps
```

Запуск приложения в контейнерах (БД + app):
```bash
docker compose up -d
```

Остановка:
```bash
docker compose down
```

Для разработки в IDE подключён `spring-boot-devtools` (auto-restart при сохранении,
если включён Build Automatically).

**Важно:** Проект использует Spring Boot 4.0.5 и Java 25.

---

## Тесты

- **`AuditEventIntegrationTest`** — полный цикл `POST`/`GET` через MockMvc +
  Testcontainers Postgres. Проверяет: happy path, валидацию, server-side timestamp,
  hash chain, все фильтры поиска.
- **`RetentionServiceIntegrationTest`** — проверяет, что события старше retention window
  копируются в архив, но **остаются** в горячей таблице (compliance требование).

Testcontainers поднимает `postgres:16-alpine` автоматически — Docker должен быть запущен.

**Принципы тестирования audit системы**: 
- НЕ используйте cleanup в `@BeforeEach` - audit данные не должны удаляться
- Используйте уникальные данные в тестах (например, `System.currentTimeMillis()` в actor)
- Проверяйте относительные изменения count'ов, а не абсолютные значения
- Тесты должны отражать реальную работу системы без нарушения audit trail

---

## Troubleshooting — Частые проблемы

### 1. Hash chain нарушена после восстановления из бэкапа
**Диагностика:** Несоответствие между `previousHash` и реальными хэшами.
**Решение:** Пересчёт невозможен через приложение — триггер запрещает `UPDATE`. Требуется временное отключение триггера на уровне БД (`ALTER TABLE audit_events DISABLE TRIGGER ALL`) в рамках контролируемой процедуры восстановления, с последующим включением. Менять `computeHash` или формат канонической строки нельзя — это breaking change для всех существующих записей.

### 2. Медленные запросы поиска
**Диагностика:** Отсутствуют индексы на часто используемых полях.
**Решение:** В `V1` уже созданы индексы на `actor`, `resource`, `timestamp`, `action`. 
Для новых полей добавлять индексы в новой миграции.

### 3. Ошибки Testcontainers
**Диагностика:** Docker не запущен или недоступен.
**Решение:** Запустить Docker Desktop/daemon перед `mvnw test`.

---

## Соглашения для агентов

- Не добавлять `UPDATE`/`DELETE` эндпоинты — это нарушает compliance-требования.
- Не трогать поле `timestamp` в DTO — его там нет намеренно.
- При добавлении полей в `AuditEvent` — обязательно добавить `updatable = false`
  и новую Flyway-миграцию.
- Hash-функцию (`SHA-256`) и формат канонической строки не менять без пересчёта
  существующих хэшей (breaking change для tamper-evidence).
- Ошибки возвращать в формате RFC 7807 `ProblemDetail` через `GlobalExceptionHandler`.
- После значимых изменений обязательно обновлять `README.md` в разделе
  `## 📅 Change History` в формате: `### YYYY-MM-DD` + короткие буллеты
  "что сделано" (без воды, 1 строка на пункт).
- **ВАЖНО**: Audit данные НИКОГДА не удаляются — это требование compliance и регуляторов.
  Retention архивация = копирование в `archived_audit_events`, НЕ удаление.
