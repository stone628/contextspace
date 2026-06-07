package dev.stoneworks.contextspace.dao

import dev.stoneworks.contextspace.tables.UsersContent
import dev.stoneworks.contextspace.tables.UserRow
import dev.stoneworks.contextspace.tables.Users
import dev.stoneworks.common.util.DateTimeUtil
import dev.stoneworks.common.util.retryTransaction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.LocalDateTime

object UserDao {

    private fun toRow(it: ResultRow) = it[Users.content].let { c ->
        UserRow(
            id = it[Users.id],
            username = it[Users.username],
            passwordHash = c.passwordHash,
            nickname = c.nickname.ifBlank { it[Users.username] },
            createdAt = it[Users.createdAt],
            refreshToken = c.refreshToken,
            refreshTokenExpiresAt = c.refreshTokenExpiresAt?.let(DateTimeUtil::fromEpochMillis),
            refreshTokenRevoked = c.refreshTokenRevoked,
        )
    }

    private fun toContent(row: UserRow) = UsersContent(
        passwordHash = row.passwordHash,
        nickname = row.nickname,
        refreshToken = row.refreshToken,
        refreshTokenExpiresAt = row.refreshTokenExpiresAt?.let(DateTimeUtil::toEpochMillis),
        refreshTokenRevoked = row.refreshTokenRevoked,
    )

    suspend fun findByUsername(username: String): UserRow? = suspendTransaction(readOnly = true) {
        Users.selectAll().where(Users.username eq username).singleOrNull()?.let(::toRow)
    }

    suspend fun create(username: String, passwordHash: String): UserRow = retryTransaction {
        val createdAt = DateTimeUtil.now()
        val content = UsersContent(passwordHash = passwordHash, nickname = username)
        val insert = Users.insert {
            it[Users.username] = username
            it[Users.content] = content
            it[Users.createdAt] = createdAt
        }
        UserRow(
            id = insert[Users.id],
            username = username,
            passwordHash = content.passwordHash,
            nickname = content.nickname,
            createdAt = createdAt,
            refreshToken = content.refreshToken,
            refreshTokenExpiresAt = content.refreshTokenExpiresAt?.let(DateTimeUtil::fromEpochMillis),
            refreshTokenRevoked = content.refreshTokenRevoked,
        )
    }

    suspend fun findById(id: Long): UserRow? = suspendTransaction(readOnly = true) {
        Users.selectAll().where(Users.id eq id).singleOrNull()?.let(::toRow)
    }

    suspend fun saveRefreshToken(userId: Long, token: String, expiresAt: LocalDateTime) = retryTransaction {
        val row = findById(userId) ?: return@retryTransaction
        val updated = toContent(row).copy(
            refreshToken = token,
            refreshTokenExpiresAt = DateTimeUtil.toEpochMillis(expiresAt),
            refreshTokenRevoked = false,
        )
        Users.update({ Users.id eq userId }) {
            it[content] = updated
        }
    }

    suspend fun updateNickname(userId: Long, nickname: String): UserRow? = retryTransaction {
        val row = findById(userId) ?: return@retryTransaction null
        val updated = toContent(row).copy(nickname = nickname)
        Users.update({ Users.id eq userId }) {
            it[content] = updated
        }
        row.copy(nickname = nickname)
    }

    suspend fun revokeRefreshToken(userId: Long) = retryTransaction {
        val row = findById(userId) ?: return@retryTransaction
        val updated = toContent(row).copy(refreshTokenRevoked = true)
        Users.update({ Users.id eq userId }) {
            it[content] = updated
        }
    }
}
