package dev.stoneworks.contextspace.util

import dev.stoneworks.contextspace.auth.JwtUtils
import dev.stoneworks.contextspace.models.ErrorResponse
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.utils.io.KtorDsl
import kotlinx.coroutines.delay
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

suspend fun <T> retryTransaction(
    maxRetries: Int = 3,
    baseDelayMs: Long = 50,
    block: suspend Transaction.() -> T,
): T {
    repeat(maxRetries - 1) { attempt ->
        try {
            return newSuspendedTransaction { block() }
        } catch (_: ExposedSQLException) {
            delay(baseDelayMs shl attempt)
        }
    }
    return newSuspendedTransaction { block() }
}

fun ApplicationCall.userId(): Long? {
    val header = request.header("Authorization") ?: return null
    val token = header.removePrefix("Bearer ").trim()

    if (token.isBlank()) return null

    val decoded = JwtUtils.verifyAuthToken(token) ?: return null

    return if (JwtUtils.isExpired(decoded, DateTimeUtil.now())) null else decoded.subject?.toLongOrNull()
}

@KtorDsl
fun Route.authGet(path: String, body: suspend RoutingContext.(userId: Long) -> Unit): Route {
    return route(path, HttpMethod.Get) {
        handle {
            val userId = call.userId() ?: run {
                return@handle call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing or invalid auth token"))
            }

            body(this, userId)
        }
    }
}

@KtorDsl
fun Route.authPut(path: String, body: suspend RoutingContext.(userId: Long) -> Unit): Route {
    return route(path, HttpMethod.Put) {
        handle {
            val userId = call.userId() ?: run {
                return@handle call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing or invalid auth token"))
            }

            body(this, userId)
        }
    }
}

@KtorDsl
inline fun <reified T: Any> Route.authPut(path: String, crossinline body: suspend RoutingContext.(userId: Long, req: T) -> Unit): Route {
    return route(path, HttpMethod.Put) {
        handle {
            val req = try { call.receive<T>() } catch (e: Exception) {
                return@handle call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid JSON request"))
            }

            val userId = call.userId() ?: run {
                return@handle call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing or invalid auth token"))
            }

            body(this, userId, req)
        }
    }
}

@KtorDsl
inline fun <reified T: Any> Route.post(path: String, crossinline body: suspend RoutingContext.(req: T) -> Unit): Route {
    return route(path, HttpMethod.Post) {
        handle {
            val req = try { call.receive<T>() } catch (e: Exception) {
                return@handle call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid JSON request"))
            }

            body(this, req)
        }
    }
}
