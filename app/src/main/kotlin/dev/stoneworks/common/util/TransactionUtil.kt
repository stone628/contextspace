package dev.stoneworks.common.util

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

