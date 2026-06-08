package dev.stoneworks.contextspace

import dev.stoneworks.common.common
import dev.stoneworks.common.util.InvalidParameterException
import dev.stoneworks.common.util.InvalidRequestException
import dev.stoneworks.common.util.UnauthorizedException
import dev.stoneworks.contextspace.models.ErrorResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    common()

    install(StatusPages) {
        exception<UnauthorizedException> { call, cause ->
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse(cause.message!!))
        }
        exception<InvalidRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message!!))
        }
        exception<InvalidParameterException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message!!))
        }
        exception<Throwable> { call, cause ->
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse(cause.message ?: "Internal error"))
        }
    }
}
