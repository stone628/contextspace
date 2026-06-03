package dev.stoneworks.contextspace

import dev.stoneworks.contextspace.auth.JwtUtils
import dev.stoneworks.contextspace.auth.PasswordHasher
import dev.stoneworks.contextspace.auth.authRoutes
import dev.stoneworks.contextspace.models.AuthResponse
import dev.stoneworks.contextspace.models.ErrorResponse
import dev.stoneworks.contextspace.models.LoginRequest
import dev.stoneworks.contextspace.models.RefreshRequest
import dev.stoneworks.contextspace.tables.Users
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
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
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Internal error"))
        }
    }

    routing {
        authRoutes()
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
                    it[passwordHash] = PasswordHasher.hash("testpass")
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
}
