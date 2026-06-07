package dev.stoneworks.common.util

import dev.stoneworks.common.component.JwtUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlin.reflect.KClass

private fun userId(call: ApplicationCall): Long? {
    val header = call.request.header("Authorization") ?: return null
    val token = header.removePrefix("Bearer ").trim()

    if (token.isBlank()) return null

    val decoded = JwtUtils.verifyAuthToken(token) ?: return null

    return if (JwtUtils.isExpired(decoded, DateTimeUtil.now())) null else decoded.subject?.toLongOrNull()
}

class UnauthorizedException(val method: HttpMethod, val path: String) : RuntimeException("Missing or invalid auth token")
class InvalidRequestException(val method: HttpMethod, val path: String, cause: Exception) : RuntimeException("Invalid JSON request", cause)

fun <T : Any> Route.method(method: HttpMethod, path: String, reqClass: KClass<T>, body: suspend RoutingContext.(req: T) -> Unit) =
    route(path, method) {
        handle {
            val req = try { call.receive(reqClass) } catch (e: Exception) {
                throw InvalidRequestException(HttpMethod.Put, path, e)
            }

            body(this, req)
        }
    }

fun Route.authMethod(method: HttpMethod, path: String, body: suspend RoutingContext.(userId: Long) -> Unit) =
    route(path, method) {
        handle {
            val userId = userId(call) ?: throw UnauthorizedException(HttpMethod.Get, path)

            body(this, userId)
        }
    }

fun <T: Any> Route.authMethod(method: HttpMethod, path: String, reqClass: KClass<T>, body: suspend RoutingContext.(userId: Long, req: T) -> Unit) =
    route(path, method) {
        handle {
            val req = try { call.receive(reqClass) } catch (e: Exception) {
                throw InvalidRequestException(HttpMethod.Put, path, e)
            }

            val userId = userId(call) ?: throw UnauthorizedException(HttpMethod.Get, path)

            body(this, userId, req)
        }
    }

@KtorDsl
inline fun <reified T: Any> Route.get(path: String, noinline body: suspend RoutingContext.(req: T) -> Unit) = method(HttpMethod.Get, path, T::class, body)

@KtorDsl
inline fun <reified T: Any> Route.put(path: String, noinline body: suspend RoutingContext.(req: T) -> Unit) = method(HttpMethod.Put, path, T::class, body)

@KtorDsl
inline fun <reified T: Any> Route.post(path: String, noinline body: suspend RoutingContext.(req: T) -> Unit) = method(HttpMethod.Post, path, T::class, body)

@KtorDsl
fun Route.authGet(path: String, body: suspend RoutingContext.(userId: Long) -> Unit) = authMethod(HttpMethod.Get, path, body)
@KtorDsl
inline fun <reified T: Any> Route.authGet(path: String, noinline body: suspend RoutingContext.(userId: Long, req: T) -> Unit) = authMethod(HttpMethod.Get, path, T::class, body)

@KtorDsl
fun Route.authPut(path: String, body: suspend RoutingContext.(userId: Long) -> Unit) = authMethod(HttpMethod.Put, path, body)
@KtorDsl
inline fun <reified T: Any> Route.authPut(path: String, noinline body: suspend RoutingContext.(userId: Long, req: T) -> Unit) = authMethod(HttpMethod.Put, path, T::class, body)

@KtorDsl
fun Route.authPost(path: String, body: suspend RoutingContext.(userId: Long) -> Unit) = authMethod(HttpMethod.Post, path, body)


