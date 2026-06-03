# TODO — Kotlin Ktor REST API Server

**Namespace:** `dev.stoneworks.contextspace`

- [x] Scaffold Gradle project with Kotlin DSL, apply Ktor + kotlinx.serialization + Exposed plugins
- [x] Configure application.yaml (Ktor server, DB, Redis, JWT settings)
- [x] Define database tables (users, refresh_tokens) via Exposed DSL
- [x] Implement user data layer: repository/DAO for user lookup & password verification
- [x] Implement JWT utility: sign/verify auth token (5min) and refresh token (1wk)
- [x] Implement refresh token data layer: store/revoke/validate refresh tokens in DB or Redis
- [x] Build POST /auth/login route: validate credentials, issue auth+refresh tokens
- [x] Build POST /auth/refresh route: accept auth token (non-expired) or refresh token, issue new auth token
- [x] Wire up Ktor module: install ContentNegotiation (JSON), StatusPages, Routing
- [x] Add Redis plugin for refresh token caching/blacklist
- [x] Add PostgreSQL plugin with HikariCP connection pool
- [x] Implement password hashing (BCrypt) for user storage
- [x] Create database migration / schema init script (Exposed auto-create)
- [x] Write integration tests for login and refresh flows
- [x] Create Gradle wrapper and verify clean build
