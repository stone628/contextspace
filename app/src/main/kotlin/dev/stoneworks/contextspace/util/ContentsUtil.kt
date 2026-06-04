package dev.stoneworks.contextspace.util

import dev.stoneworks.contextspace.auth.JwtUtils
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
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

fun ApplicationCall.authUserId(): Long? {
    val header = request.header("Authorization") ?: return null
    val token = header.removePrefix("Bearer ").trim()
    if (token.isBlank()) return null
    val decoded = JwtUtils.verifyAuthToken(token) ?: return null
    if (JwtUtils.isExpired(decoded)) return null
    return decoded.subject?.toLongOrNull()
}
