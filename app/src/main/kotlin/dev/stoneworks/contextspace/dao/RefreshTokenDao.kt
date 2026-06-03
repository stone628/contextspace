package dev.stoneworks.contextspace.dao

import dev.stoneworks.contextspace.tables.RefreshTokenRow
import dev.stoneworks.contextspace.tables.RefreshTokens
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

object RefreshTokenDao {

    suspend fun findByToken(token: String): RefreshTokenRow? = newSuspendedTransaction {
        RefreshTokens.selectAll().where { RefreshTokens.token eq token }.singleOrNull()?.let {
            RefreshTokenRow(
                id = it[RefreshTokens.id],
                token = it[RefreshTokens.token],
                userId = it[RefreshTokens.userId],
                expiresAt = it[RefreshTokens.expiresAt],
                revoked = it[RefreshTokens.revoked],
            )
        }
    }

    suspend fun create(token: String, userId: Long, expiresAt: LocalDateTime) = newSuspendedTransaction {
        RefreshTokens.insert {
            it[RefreshTokens.token] = token
            it[RefreshTokens.userId] = userId
            it[RefreshTokens.expiresAt] = expiresAt
        }
    }

    suspend fun revoke(tokenId: Long) = newSuspendedTransaction {
        RefreshTokens.update({ RefreshTokens.id eq tokenId }) {
            it[revoked] = true
        }
    }

    suspend fun revokeAllForUser(userId: Long) = newSuspendedTransaction {
        RefreshTokens.update({
            (RefreshTokens.userId eq userId) and (RefreshTokens.revoked eq false)
        }) {
            it[revoked] = true
        }
    }
}
