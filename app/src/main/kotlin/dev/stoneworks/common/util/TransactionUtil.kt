package dev.stoneworks.common.util

import kotlinx.coroutines.delay
import org.jetbrains.exposed.v1.core.Transaction
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

suspend fun <T> retryTransaction(
    maxRetries: Int = 3,
    baseDelayMs: Long = 50,
    block: suspend Transaction.() -> T,
): T {
    repeat(maxRetries - 1) { attempt ->
        try {
            return suspendTransaction { block() }
        } catch (_: ExposedSQLException) {
            delay(baseDelayMs shl attempt)
        }
    }
    return suspendTransaction { block() }
}

