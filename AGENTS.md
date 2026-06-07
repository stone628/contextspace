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
| `app/build.gradle.kts` | Ktor 3.5 + kotlinx.serialization + Exposed 0.57 + BCrypt |
| `app/src/main/kotlin/dev/stoneworks/contextspace/` | Server source root |
| `.../auth/` | Route definitions (`AuthRoutes`, `AccountRoutes`) |
| `.../dao/` | Repository layer (`UserDao`) |
| `.../models/` | Request/response DTOs |
| `.../tables/` | Exposed table definitions (`Users`) |
| `app/src/main/kotlin/dev/stoneworks/common/component/` | Shared components (`JwtUtils`) |
| `.../common/util/` | Shared utilities: `KtorUtil` (typed route builders), `StringUtil` (BCrypt), `DateTimeUtil`, `TransactionUtil` (`retryTransaction`), `RateLimiter` (Redis-backed) |
| `docker/env_dev.yml` | Valkey + PostgreSQL for local dev |

## Table schema policy

See [agents/TABLE_SCHEMA.md](agents/TABLE_SCHEMA.md).

## API

- `POST /auth/register` — `{ username, password }` → `{ authToken, refreshToken }` (201)
- `POST /auth/login` — `{ username, password }` → `{ authToken, refreshToken }`
- `POST /auth/refresh` — `{ authToken }` or `{ refreshToken }` → `{ authToken, refreshToken }`
- `POST /auth/logout` — revokes refresh token (auth required)
- `GET /account/profile` — `{ username, nickname }` (auth required)
- `PUT /account/profile` — `{ nickname }` → `{ username, nickname }` (auth required)

Auth token expires in 5 min, refresh token in 7 days.

Routes use typed builders: `post<T>`, `authGet`, `authPut`. Auth helpers extract the `userId` from the `Authorization: Bearer <token>` header. JWT token methods accept `LocalDateTime now` for deterministic testability.

Auth endpoints are rate-limited via Redis (register: 5/min, login: 10/min, refresh: 20/min per IP). CORS is enabled for all origins with standard headers and methods.

## Infrastructure

Run `docker compose -f docker/env_dev.yml up` for PostgreSQL and Valkey.
Config in `application.yaml` at classpath root.
