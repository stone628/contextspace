# TODO — Kotlin Ktor REST API Server

**Namespace:** `dev.stoneworks.contextspace`

- [x] Scaffold Gradle project with Kotlin DSL, apply Ktor + kotlinx.serialization + Exposed plugins
- [x] Configure application.yaml (Ktor server, DB, Redis, JWT settings)
- [x] Define Users table with JSONB content encoding all non-indexed fields (refresh tokens live in content)
- [x] Implement UserDao with `toRow`/`toContent` mapping for JSONB document
- [x] Implement JWT utility: sign/verify auth token (5min) and refresh token (1wk), uses `LocalDateTime now` for deterministic testing
- [x] Implement refresh token persistence in Users JSONB (save, revoke, validate)
- [x] Build POST /auth/login route: validate credentials, issue auth+refresh tokens
- [x] Build POST /auth/refresh route: accept auth token (non-expired) or refresh token, issue new token pair
- [x] Build POST /auth/register route: create user, return auth+refresh tokens (201)
- [x] Build GET /account/profile route: return username + nickname (auth required)
- [x] Build PUT /account/profile route: update nickname (auth required)
- [x] Typed route builders: `post<T>`, `authGet`, `authPut` — extract userId from Bearer token
- [x] Wire up Ktor module: ContentNegotiation (JSON), StatusPages, Routing
- [x] Add PostgreSQL plugin with HikariCP connection pool (repeatable read, auto-commit off)
- [x] Implement password hashing (BCrypt cost 12) for user storage
- [x] Create database schema via `SchemaUtils.create(Users)` on startup
- [x] Write integration tests for auth and account flows (H2 in-memory)
- [x] Write unit tests for JWT utils and password hasher
- [x] Create Gradle wrapper and verify clean build

- [x] Wire Redis into application logic — `RateLimiter` uses Redis INCR+EXPIRE for sliding-window rate limiting on auth endpoints
- [x] Add logout endpoint (`POST /auth/logout`) that revokes the user's refresh token
- [x] Add rate limiting on auth endpoints (register: 5/min, login: 10/min, refresh: 20/min per IP)
- [x] Add CORS for frontend access (all origins, standard headers + methods)

- [ ] Add CI configuration (`.github/workflows/`)
- [ ] Add username/password validation rules (min length, character constraints)
