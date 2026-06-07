package dev.stoneworks.contextspace

import dev.stoneworks.common.component.JwtUtils
import dev.stoneworks.common.util.InvalidRequestException
import dev.stoneworks.common.util.UnauthorizedException
import dev.stoneworks.common.util.fromConfig
import dev.stoneworks.contextspace.auth.accountRoutes
import dev.stoneworks.contextspace.auth.authRoutes
import dev.stoneworks.contextspace.models.ErrorResponse
import dev.stoneworks.contextspace.tables.Users
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.CORS
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

    monitor.subscribe(ApplicationStopping) {
        DatabaseConfig.close()
        RedisConfig.close()
    }

    runBlocking {
        newSuspendedTransaction {
            SchemaUtils.create(Users)
        }
    }

    install(CORS) {
        fromConfig(config)
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
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
