package dev.stoneworks.contextspace.auth

import dev.stoneworks.contextspace.dao.UserDao
import dev.stoneworks.contextspace.models.ErrorResponse
import dev.stoneworks.contextspace.models.ProfileResponse
import dev.stoneworks.contextspace.models.UpdateNicknameRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put

private fun ApplicationCall.authenticatedUserId(): Long? {
    val header = request.header("Authorization") ?: return null
    val token = header.removePrefix("Bearer ").trim()
    if (token.isBlank()) return null
    val decoded = JwtUtils.verifyAuthToken(token) ?: return null
    if (JwtUtils.isExpired(decoded)) return null
    return decoded.subject?.toLongOrNull()
}

fun Route.accountRoutes() {
    get("/account/profile") {
        val userId = call.authenticatedUserId() ?: run {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing or invalid auth token"))
            return@get
        }
        val user = UserDao.findById(userId)
            ?: run {
                call.respond(HttpStatusCode.Unauthorized, ErrorResponse("User not found"))
                return@get
            }
        call.respond(ProfileResponse(username = user.username, nickname = user.nickname))
    }

    put("/account/profile") {
        val userId = call.authenticatedUserId() ?: run {
            call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing or invalid auth token"))
            return@put
        }
        val request = call.receive<UpdateNicknameRequest>()
        if (request.nickname.isBlank()) {
            call.respond(HttpStatusCode.BadRequest, ErrorResponse("Nickname must not be empty"))
            return@put
        }
        UserDao.updateNickname(userId, request.nickname)
        val user = UserDao.findById(userId)!!
        call.respond(ProfileResponse(username = user.username, nickname = user.nickname))
    }
}
