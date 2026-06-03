package dev.stoneworks.contextspace

import dev.stoneworks.contextspace.auth.JwtUtils
import dev.stoneworks.contextspace.auth.authRoutes
import dev.stoneworks.contextspace.models.ErrorResponse
import dev.stoneworks.contextspace.tables.RefreshTokens
import dev.stoneworks.contextspace.tables.Users
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val config = environment.config

    DatabaseConfig.init(config)
    JwtUtils.init(config)
    RedisConfig.init(config)

    runBlocking {
        newSuspendedTransaction {
            SchemaUtils.create(Users, RefreshTokens)
        }
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
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
