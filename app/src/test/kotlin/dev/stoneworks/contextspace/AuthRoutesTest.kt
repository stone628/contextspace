package dev.stoneworks.contextspace

import dev.stoneworks.common.component.JwtUtils
import dev.stoneworks.common.util.InvalidRequestException
import dev.stoneworks.common.util.StringUtil
import dev.stoneworks.common.util.UnauthorizedException
import dev.stoneworks.contextspace.auth.accountRoutes
import dev.stoneworks.contextspace.auth.authRoutes
import dev.stoneworks.contextspace.models.AuthResponse
import dev.stoneworks.contextspace.models.ErrorResponse
import dev.stoneworks.contextspace.models.LoginRequest
import dev.stoneworks.contextspace.models.ProfileResponse
import dev.stoneworks.contextspace.models.RefreshRequest
import dev.stoneworks.contextspace.models.RegisterRequest
import dev.stoneworks.contextspace.models.UpdateNicknameRequest
import dev.stoneworks.contextspace.tables.UsersContent
import dev.stoneworks.contextspace.tables.Users
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private val jwtConfig = MapApplicationConfig(
    "jwt.authSecret" to "test-auth-secret",
    "jwt.authIssuer" to "test",
    "jwt.authExpiresInMinutes" to "5",
    "jwt.refreshSecret" to "test-refresh-secret",
    "jwt.refreshIssuer" to "test",
    "jwt.refreshExpiresInDays" to "7",
)

private fun Application.testModule() {
    JwtUtils.init(jwtConfig)

    install(ServerContentNegotiation) {
        json(Json { ignoreUnknownKeys = true; isLenient = true })
    }

    install(StatusPages) {
        exception<UnauthorizedException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(cause.message!!))
        }
        exception<InvalidRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message!!))
        }
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Internal error"))
        }
    }

    routing {
        authRoutes()
        accountRoutes()
    }
}

class AuthRoutesTest {

    companion object {
        @BeforeAll
        @JvmStatic
        fun setupDatabase() {
            DatabaseConfig.init(
                MapApplicationConfig(
                    "database.url" to "jdbc:h2:mem:test-auth;DB_CLOSE_DELAY=-1",
                    "database.driver" to "org.h2.Driver",
                    "database.user" to "sa",
                    "database.password" to "",
                    "database.poolSize" to "2",
                )
            )
            transaction {
                SchemaUtils.create(Users)
                Users.insert {
                    it[username] = "testuser"
                    it[content] = UsersContent(passwordHash = StringUtil.hashPassword("testpass"))
                }
            }
        }

        @AfterAll
        @JvmStatic
        fun cleanupDatabase() {
            DatabaseConfig.close()
        }
    }

    @Test
    fun `login with valid credentials returns tokens`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "testuser", password = "testpass"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<AuthResponse>()
        assertNotNull(body.authToken)
        assertNotNull(body.refreshToken)
    }

    @Test
    fun `login with invalid credentials returns unauthorized`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "testuser", password = "wrongpass"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `login with unknown user returns unauthorized`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "nobody", password = "testpass"))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `refresh with valid auth token returns new tokens`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "testuser", password = "testpass"))
        }
        val loginBody = loginResponse.body<AuthResponse>()

        val refreshResponse = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(authToken = loginBody.authToken))
        }

        assertEquals(HttpStatusCode.OK, refreshResponse.status)
        val refreshBody = refreshResponse.body<AuthResponse>()
        assertNotNull(refreshBody.authToken)
        assertNotNull(refreshBody.refreshToken)
    }

    @Test
    fun `refresh with valid refresh token returns new tokens`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "testuser", password = "testpass"))
        }
        val loginBody = loginResponse.body<AuthResponse>()

        val refreshResponse = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken = loginBody.refreshToken))
        }

        assertEquals(HttpStatusCode.OK, refreshResponse.status)
        val refreshBody = refreshResponse.body<AuthResponse>()
        assertNotNull(refreshBody.authToken)
        assertNotNull(refreshBody.refreshToken)
    }

    @Test
    fun `refresh with no token returns bad request`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest())
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `get profile without token returns unauthorized`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.get("/account/profile")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `get profile with valid token returns username and nickname`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val registerResponse = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequest(username = "profileuser", password = "testpass"))
        }
        val registerBody = registerResponse.body<AuthResponse>()

        val response = client.get("/account/profile") {
            bearerAuth(registerBody.authToken)
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<ProfileResponse>()
        assertEquals("profileuser", body.username)
        assertEquals("profileuser", body.nickname)
    }

    @Test
    fun `update nickname with valid token returns updated profile`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "testuser", password = "testpass"))
        }
        val loginBody = loginResponse.body<AuthResponse>()

        val response = client.put("/account/profile") {
            bearerAuth(loginBody.authToken)
            contentType(ContentType.Application.Json)
            setBody(UpdateNicknameRequest(nickname = "NewNick"))
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.body<ProfileResponse>()
        assertEquals("testuser", body.username)
        assertEquals("NewNick", body.nickname)
    }

    @Test
    fun `update nickname without token returns unauthorized`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.put("/account/profile") {
            contentType(ContentType.Application.Json)
            setBody(UpdateNicknameRequest(nickname = "NewNick"))
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `update nickname with empty nickname returns bad request`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "testuser", password = "testpass"))
        }
        val loginBody = loginResponse.body<AuthResponse>()

        val response = client.put("/account/profile") {
            bearerAuth(loginBody.authToken)
            contentType(ContentType.Application.Json)
            setBody(UpdateNicknameRequest(nickname = ""))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `logout with valid token returns ok`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val loginResponse = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(username = "testuser", password = "testpass"))
        }
        val loginBody = loginResponse.body<AuthResponse>()

        val response = client.post("/auth/logout") {
            bearerAuth(loginBody.authToken)
        }
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `logout without token returns unauthorized`() = testApplication {
        application { testModule() }

        val client = createClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.post("/auth/logout")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
