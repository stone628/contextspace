package dev.stoneworks.contextspace.auth

import dev.stoneworks.common.util.authGet
import dev.stoneworks.common.util.authPut
import dev.stoneworks.contextspace.dao.UserDao
import dev.stoneworks.contextspace.models.ErrorResponse
import dev.stoneworks.contextspace.models.ProfileResponse
import dev.stoneworks.contextspace.models.UpdateNicknameRequest
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.accountRoutes() {
    authGet("/account/profile") { userId ->
        val user = UserDao.findById(userId) ?: run {
            return@authGet call.respond(HttpStatusCode.Unauthorized, ErrorResponse("User not found"))
        }

        call.respond(ProfileResponse(username = user.username, nickname = user.nickname))
    }

    authPut<UpdateNicknameRequest>("/account/profile") { userId, request ->
        if (request.nickname.isBlank()) {
            return@authPut call.respond(HttpStatusCode.BadRequest, ErrorResponse("Nickname must not be empty"))
        }

        val user = UserDao.updateNickname(userId, request.nickname) ?: run {
            return@authPut call.respond(HttpStatusCode.Unauthorized, ErrorResponse("User not found"))
        }

        call.respond(ProfileResponse(username = user.username, nickname = user.nickname))
    }
}
