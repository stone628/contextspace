# AGENTS.md

Kotlin/Ktor REST API server at `app/`.

## Quick start

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 20)
./gradlew build        # compile + test
./gradlew test         # run tests (H2 in-memory, no infra needed)
```

## Project structure

| Path | Purpose |
|---|---|
| `app/build.gradle.kts` | Ktor 3.1 + kotlinx.serialization + Exposed 0.57 + BCrypt |
| `app/src/main/kotlin/dev/stoneworks/contextspace/` | Source root |
| `.../tables/` | Exposed table definitions (`users`) |
| `.../dao/` | Repository layer (`UserDao`) |
| `.../auth/` | JWT utils, password hasher, route handlers |
| `.../models/` | Request/response DTOs |
| `docker/env_dev.yml` | Valkey + PostgreSQL for local dev |

## Table schema policy

Every table follows the same pattern — a JSONB document per row storing all non-indexed fields.

| Column | Purpose |
|---|---|
| `id` | `BIGINT PK`, auto-increment |
| `<index_column>` | One or more indexed lookup columns (`VARCHAR`, unique if appropriate) |
| `content` | `JSONB` — all remaining fields bundled in a `{TableName}Content` data class |
| `created_at` | `DATETIME`, default `now()` |

Three classes are tied together for each table:

| Class | File | Role |
|---|---|---|
| `{TableName}Content` | `tables/{TableName}.kt` | `@Serializable` — exact shape of the JSONB document |
| `{TableName}` (table) | `tables/{TableName}.kt` | Exposed table with `id`, index columns, `jsonb<...>("content", ...)`, `createdAt` |
| `{TableName}Row` | `tables/{TableName}.kt` | In-memory row — **must mirror every field in `{TableName}Content`** as top-level fields, plus `createdAt: java.time.LocalDateTime` |

**When a field is added, removed, or renamed in `{TableName}Content`, the same change must be applied to `{TableName}Row`.** The DAO's `toRow` and `toContent` mapping functions are the bridge — `toRow` destructures the content class into the row's expanded fields, `toContent` folds the row back into the content class. Keep the two in sync.

Steps to add a new table (e.g. `widgets`):

1. Create `tables/Widgets.kt` — `object Widgets : Table("widgets")` with `id`, index columns, `jsonb<WidgetsContent>("content", Json.Default)`, and `createdAt`. Add `WidgetsRow` with the same fields as `WidgetsContent` expanded at the top level. Add `WidgetsContent` — a `@Serializable` data class with all non-indexed fields (use `Long?` for timestamps, not `LocalDateTime`).
2. Create `dao/WidgetDao.kt` — read/write `it[Widgets.content]` as the whole document, map via `toRow`/`toContent`.
3. Register in `Application.kt` via `SchemaUtils.create(Widgets, ...)`.

## API

- `POST /auth/register` — `{ username, password }` → `{ authToken, refreshToken }` (201)
- `POST /auth/login` — `{ username, password }` → `{ authToken, refreshToken }`
- `POST /auth/refresh` — `{ authToken }` or `{ refreshToken }` → `{ authToken, refreshToken }`

Auth token expires in 5 min, refresh token in 7 days.

## Infrastructure

Run `docker compose -f docker/env_dev.yml up` for PostgreSQL and Valkey.
Config in `application.yaml` at classpath root.
