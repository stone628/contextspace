package dev.stoneworks.contextspace.dao

import dev.stoneworks.contextspace.tables.UsersContent
import dev.stoneworks.contextspace.tables.UserRow
import dev.stoneworks.contextspace.tables.Users
import dev.stoneworks.contextspace.util.DateTimeUtil
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

object UserDao {

    private fun toRow(it: ResultRow) = it[Users.content].let { c ->
        UserRow(
            id = it[Users.id],
            username = it[Users.username],
            passwordHash = c.passwordHash,
            createdAt = it[Users.createdAt],
            refreshToken = c.refreshToken,
            refreshTokenExpiresAt = c.refreshTokenExpiresAt?.let(DateTimeUtil::fromEpochMillis),
            refreshTokenRevoked = c.refreshTokenRevoked,
        )
    }

    private fun toContent(row: UserRow) = UsersContent(
        passwordHash = row.passwordHash,
        refreshToken = row.refreshToken,
        refreshTokenExpiresAt = row.refreshTokenExpiresAt?.let(DateTimeUtil::toEpochMillis),
        refreshTokenRevoked = row.refreshTokenRevoked,
    )

    suspend fun findByUsername(username: String): UserRow? = newSuspendedTransaction {
        Users.selectAll().where(Users.username eq username).singleOrNull()?.let(::toRow)
    }

    suspend fun create(username: String, passwordHash: String): UserRow = newSuspendedTransaction {
        val createdAt = DateTimeUtil.now()
        val content = UsersContent(passwordHash = passwordHash)
        val insert = Users.insert {
            it[Users.username] = username
            it[Users.content] = content
            it[Users.createdAt] = createdAt
        }
        UserRow(
            id = insert[Users.id],
            username = username,
            passwordHash = content.passwordHash,
            createdAt = createdAt,
            refreshToken = content.refreshToken,
            refreshTokenExpiresAt = content.refreshTokenExpiresAt?.let(DateTimeUtil::fromEpochMillis),
            refreshTokenRevoked = content.refreshTokenRevoked,
        )
    }

    suspend fun findById(id: Long): UserRow? = newSuspendedTransaction {
        Users.selectAll().where(Users.id eq id).singleOrNull()?.let(::toRow)
    }

    suspend fun saveRefreshToken(userId: Long, token: String, expiresAt: LocalDateTime) = newSuspendedTransaction {
        val row = findById(userId) ?: return@newSuspendedTransaction
        val updated = toContent(row).copy(
            refreshToken = token,
            refreshTokenExpiresAt = DateTimeUtil.toEpochMillis(expiresAt),
            refreshTokenRevoked = false,
        )
        Users.update({ Users.id eq userId }) {
            it[content] = updated
        }
    }

    suspend fun revokeRefreshToken(userId: Long) = newSuspendedTransaction {
        val row = findById(userId) ?: return@newSuspendedTransaction
        val updated = toContent(row).copy(refreshTokenRevoked = true)
        Users.update({ Users.id eq userId }) {
            it[content] = updated
        }
    }
}
