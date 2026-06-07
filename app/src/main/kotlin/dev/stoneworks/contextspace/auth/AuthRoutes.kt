package dev.stoneworks.contextspace.auth

import dev.stoneworks.common.component.JwtUtils
import dev.stoneworks.common.util.DateTimeUtil
import dev.stoneworks.common.util.InvalidParameterException
import dev.stoneworks.common.util.RateLimiter
import dev.stoneworks.common.util.StringUtil
import dev.stoneworks.common.util.authPost
import dev.stoneworks.contextspace.dao.UserDao
import dev.stoneworks.contextspace.models.*
import io.ktor.http.*
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.*
import io.ktor.server.routing.*

private fun clientIp(call: ApplicationCall): String =
    call.request.headers["X-Forwarded-For"]?.split(",")?.first()?.trim()
        ?: "dev"

fun Route.authRoutes() {
    post<RegisterRequest>("/auth/register") { request ->
        if (RateLimiter.isLimited("rl:register:${clientIp(call)}", 5, 60)) {
            return@post call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("Too many requests"))
        }

        if (request.username.isBlank() || request.password.isBlank()) {
            throw InvalidParameterException(call, "Username and password are required")
        }

        val existing = UserDao.findByUsername(request.username)
        if (existing != null) {
            call.respond(HttpStatusCode.Conflict, ErrorResponse("Username already taken"))
            return@post
        }

        val passwordHash = StringUtil.hashPassword(request.password)
        val user = UserDao.create(request.username, passwordHash)

        val now = DateTimeUtil.now()
        val authToken = JwtUtils.generateAuthToken(user.id, user.username, now)
        val refreshToken = JwtUtils.generateRefreshToken(user.id, now)
        val expiresAt = now.plusDays(7)

        UserDao.saveRefreshToken(user.id, refreshToken, expiresAt)
        call.respond(HttpStatusCode.Created, AuthResponse(authToken = authToken, refreshToken = refreshToken))
    }

    post<LoginRequest>("/auth/login") { request ->
        if (RateLimiter.isLimited("rl:login:${clientIp(call)}", 10, 60)) {
            return@post call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("Too many requests"))
        }

        val user = UserDao.findByUsername(request.username)
            ?: run {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
                return@post
            }

        if (!StringUtil.verifyPassword(request.password, user.passwordHash)) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
            return@post
        }

        val now = DateTimeUtil.now()
        val authToken = JwtUtils.generateAuthToken(user.id, user.username, now)
        val refreshToken = JwtUtils.generateRefreshToken(user.id, now)
        val expiresAt = now.plusDays(7)

        UserDao.saveRefreshToken(user.id, refreshToken, expiresAt)
        call.respond(HttpStatusCode.OK, AuthResponse(authToken = authToken, refreshToken = refreshToken))
    }

    post<RefreshRequest>("/auth/refresh") { request ->
        if (RateLimiter.isLimited("rl:refresh:${clientIp(call)}", 20, 60)) {
            return@post call.respond(HttpStatusCode.TooManyRequests, ErrorResponse("Too many requests"))
        }

        var userId: Long? = null

        if (!request.authToken.isNullOrBlank()) {
            val decoded = JwtUtils.verifyAuthToken(request.authToken)
            if (decoded != null && !JwtUtils.isExpired(decoded, DateTimeUtil.now())) {
                userId = decoded.subject?.toLongOrNull()
            } else {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Auth token expired or invalid"))
                return@post
            }
        } else if (!request.refreshToken.isNullOrBlank()) {
            val decoded = JwtUtils.verifyRefreshToken(request.refreshToken)
            if (decoded != null && !JwtUtils.isExpired(decoded, DateTimeUtil.now())) {
                userId = decoded.subject?.toLongOrNull()
                val user = userId?.let { UserDao.findById(it) }
                if (user == null || user.refreshToken != request.refreshToken || user.refreshTokenRevoked) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Refresh token revoked or not found"))
                    return@post
                }
            } else {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Refresh token expired or invalid"))
                return@post
            }
        } else {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Provide authToken or refreshToken"))
            return@post
        }

        val user = userId?.let { UserDao.findById(it) }
            ?: run {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("User not found"))
                return@post
            }

        val now = DateTimeUtil.now()
        val newAuthToken = JwtUtils.generateAuthToken(user.id, user.username, now)
        val newRefreshToken = JwtUtils.generateRefreshToken(user.id, now)
        val expiresAt = now.plusDays(7)

        UserDao.saveRefreshToken(user.id, newRefreshToken, expiresAt)
        call.respond(HttpStatusCode.OK, AuthResponse(authToken = newAuthToken, refreshToken = newRefreshToken))
    }

    authPost("/auth/logout") { userId ->
        UserDao.revokeRefreshToken(userId)
        call.respond(HttpStatusCode.OK, ErrorResponse("Logged out"))
    }
}
