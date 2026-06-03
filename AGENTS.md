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
| `.../tables/` | Exposed table definitions (`users`, `refresh_tokens`) |
| `.../dao/` | Repository layer (`UserDao`, `RefreshTokenDao`) |
| `.../auth/` | JWT utils, password hasher, route handlers |
| `.../models/` | Request/response DTOs |
| `docker/env_dev.yml` | Valkey + PostgreSQL for local dev |

## API

- `POST /auth/login` — `{ username, password }` → `{ authToken, refreshToken }`
- `POST /auth/refresh` — `{ authToken }` or `{ refreshToken }` → `{ authToken, refreshToken }`

Auth token expires in 5 min, refresh token in 7 days.

## Infrastructure

Run `docker compose -f docker/env_dev.yml up` for PostgreSQL and Valkey.
Config in `application.yaml` at classpath root.
