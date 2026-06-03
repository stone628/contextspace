package dev.stoneworks.contextspace.auth

import dev.stoneworks.contextspace.dao.RefreshTokenDao
import dev.stoneworks.contextspace.dao.UserDao
import dev.stoneworks.contextspace.models.AuthResponse
import dev.stoneworks.contextspace.models.ErrorResponse
import dev.stoneworks.contextspace.models.LoginRequest
import dev.stoneworks.contextspace.models.RefreshRequest
import dev.stoneworks.contextspace.models.RegisterRequest
import dev.stoneworks.contextspace.util.DateTimeUtil
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.authRoutes() {
    post("/auth/register") {
        val request = call.receive<RegisterRequest>()

        if (request.username.isBlank() || request.password.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Username and password are required"))
            return@post
        }

        val existing = UserDao.findByUsername(request.username)
        if (existing != null) {
            call.respond(HttpStatusCode.Conflict, ErrorResponse("Username already taken"))
            return@post
        }

        val passwordHash = PasswordHasher.hash(request.password)
        val user = UserDao.create(request.username, passwordHash)

        val authToken = JwtUtils.generateAuthToken(user.id, user.username)
        val refreshToken = JwtUtils.generateRefreshToken(user.id)

        call.respond(HttpStatusCode.Created, AuthResponse(authToken = authToken, refreshToken = refreshToken))
    }

    post("/auth/login") {
        val request = call.receive<LoginRequest>()

        val user = UserDao.findByUsername(request.username)
            ?: run {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
                return@post
            }

        if (!PasswordHasher.verify(request.password, user.passwordHash)) {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials"))
            return@post
        }

        val authToken = JwtUtils.generateAuthToken(user.id, user.username)
        val refreshToken = JwtUtils.generateRefreshToken(user.id)

        val expiresAt = DateTimeUtil.now().plusDays(7)
        RefreshTokenDao.create(refreshToken, user.id, expiresAt)

        call.respond(HttpStatusCode.OK, AuthResponse(authToken = authToken, refreshToken = refreshToken))
    }

    post("/auth/refresh") {
        val request = call.receive<RefreshRequest>()
        var userId: Long? = null

        if (!request.authToken.isNullOrBlank()) {
            val decoded = JwtUtils.verifyAuthToken(request.authToken)
            if (decoded != null && !JwtUtils.isExpired(decoded)) {
                userId = decoded.subject?.toLongOrNull()
            } else {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Auth token expired or invalid"))
                return@post
            }
        } else if (!request.refreshToken.isNullOrBlank()) {
            val decoded = JwtUtils.verifyRefreshToken(request.refreshToken)
            if (decoded != null && !JwtUtils.isExpired(decoded)) {
                userId = decoded.subject?.toLongOrNull()
                val stored = RefreshTokenDao.findByToken(request.refreshToken)
                if (stored == null || stored.revoked) {
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

        val newAuthToken = JwtUtils.generateAuthToken(user.id, user.username)
        val newRefreshToken = JwtUtils.generateRefreshToken(user.id)
        val expiresAt = DateTimeUtil.now().plusDays(7)
        RefreshTokenDao.create(newRefreshToken, user.id, expiresAt)

        call.respond(HttpStatusCode.OK, AuthResponse(authToken = newAuthToken, refreshToken = newRefreshToken))
    }
}
