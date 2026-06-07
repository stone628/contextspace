package dev.stoneworks.common.util

import dev.stoneworks.common.component.JwtUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.cors.CORSConfig
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlin.reflect.KClass

fun CORSConfig.fromConfig(config: ApplicationConfig) {
    val allowedHosts = config.propertyOrNull("cors.allowed_hosts")?.getList() ?: emptyList()
    val allowedMethods = config.propertyOrNull("cors.allowed_methods")?.getList() ?: emptyList()
    val allowedHeaders = config.propertyOrNull("cors.allowed_headers")?.getList() ?: emptyList()

    allowCredentials = config.propertyOrNull("cors.allow_credentials")?.getString()?.toBoolean() ?: false

    // Apply allowed hosts
    allowedHosts.forEach { host ->
        if (host == "*") anyHost() else allowHost(host)
    }

    // Apply allowed methods
    allowedMethods.forEach { method ->
        allowMethod(HttpMethod.parse(method))
    }

    // Apply allowed headers
    allowedHeaders.forEach { header ->
        allowHeader(header)
    }
}

fun ApplicationCall.clientIp() = request.headers["X-Forwarded-For"]?.split(',')?.first()?.trim() ?: "dev"

private fun userId(call: ApplicationCall): Long? {
    val header = call.request.header("Authorization") ?: return null
    val token = header.removePrefix("Bearer ").trim()

    if (token.isBlank()) return null

    val decoded = JwtUtils.verifyAuthToken(token) ?: return null

    return if (JwtUtils.isExpired(decoded, DateTimeUtil.now())) null else decoded.subject?.toLongOrNull()
}

class UnauthorizedException(val method: HttpMethod, val path: String) : RuntimeException("Missing or invalid auth token")

class InvalidRequestException(val method: HttpMethod, val path: String, cause: Exception) : RuntimeException("Invalid JSON request", cause)

class InvalidParameterException(call: ApplicationCall, message: String): RuntimeException(message) {
    val method = call.request.httpMethod
    val path = call.request.path()
}

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

inline fun <reified T: Any> Route.get(path: String, noinline body: suspend RoutingContext.(req: T) -> Unit) = method(HttpMethod.Get, path, T::class, body)

inline fun <reified T: Any> Route.put(path: String, noinline body: suspend RoutingContext.(req: T) -> Unit) = method(HttpMethod.Put, path, T::class, body)

inline fun <reified T: Any> Route.post(path: String, noinline body: suspend RoutingContext.(req: T) -> Unit) = method(HttpMethod.Post, path, T::class, body)

fun Route.authGet(path: String, body: suspend RoutingContext.(userId: Long) -> Unit) = authMethod(HttpMethod.Get, path, body)
inline fun <reified T: Any> Route.authGet(path: String, noinline body: suspend RoutingContext.(userId: Long, req: T) -> Unit) = authMethod(HttpMethod.Get, path, T::class, body)

fun Route.authPut(path: String, body: suspend RoutingContext.(userId: Long) -> Unit) = authMethod(HttpMethod.Put, path, body)
inline fun <reified T: Any> Route.authPut(path: String, noinline body: suspend RoutingContext.(userId: Long, req: T) -> Unit) = authMethod(HttpMethod.Put, path, T::class, body)

fun Route.authPost(path: String, body: suspend RoutingContext.(userId: Long) -> Unit) = authMethod(HttpMethod.Post, path, body)
