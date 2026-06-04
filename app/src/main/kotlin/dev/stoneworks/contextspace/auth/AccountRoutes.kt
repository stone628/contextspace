package dev.stoneworks.contextspace.auth

import dev.stoneworks.contextspace.dao.UserDao
import dev.stoneworks.contextspace.models.ErrorResponse
import dev.stoneworks.contextspace.models.ProfileResponse
import dev.stoneworks.contextspace.models.UpdateNicknameRequest
import dev.stoneworks.contextspace.util.authUserId
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.accountRoutes() {
    get("/account/profile") {
        val userId = call.authUserId() ?: run {
            return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing or invalid auth token"))
        }
        val user = UserDao.findById(userId) ?: run {
            return@get call.respond(HttpStatusCode.Unauthorized, ErrorResponse("User not found"))
        }
        call.respond(ProfileResponse(username = user.username, nickname = user.nickname))
    }

    put("/account/profile") {
        val userId = call.authUserId() ?: run {
            return@put call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Missing or invalid auth token"))
        }
        val request = call.receive<UpdateNicknameRequest>()
        if (request.nickname.isBlank()) {
            return@put call.respond(HttpStatusCode.BadRequest, ErrorResponse("Nickname must not be empty"))
        }
        val user = UserDao.updateNickname(userId, request.nickname)
            ?: run { return@put call.respond(HttpStatusCode.Unauthorized, ErrorResponse("User not found")) }
        call.respond(ProfileResponse(username = user.username, nickname = user.nickname))
    }
}
