package dev.stoneworks.contextspace.dao

import dev.stoneworks.contextspace.tables.UserRow
import dev.stoneworks.contextspace.tables.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import java.time.LocalDateTime

object UserDao {

    private fun rowToUser(it: org.jetbrains.exposed.sql.ResultRow) = UserRow(
        id = it[Users.id],
        username = it[Users.username],
        passwordHash = it[Users.passwordHash],
        refreshToken = it[Users.refreshToken],
        refreshTokenExpiresAt = it[Users.refreshTokenExpiresAt],
        refreshTokenRevoked = it[Users.refreshTokenRevoked],
    )

    suspend fun findByUsername(username: String): UserRow? = newSuspendedTransaction {
        Users.selectAll().where(Users.username eq username).singleOrNull()?.let(::rowToUser)
    }

    suspend fun create(username: String, passwordHash: String): UserRow = newSuspendedTransaction {
        val insert = Users.insert {
            it[Users.username] = username
            it[Users.passwordHash] = passwordHash
        }
        UserRow(id = insert[Users.id], username = username, passwordHash = passwordHash)
    }

    suspend fun findById(id: Long): UserRow? = newSuspendedTransaction {
        Users.selectAll().where(Users.id eq id).singleOrNull()?.let(::rowToUser)
    }

    suspend fun saveRefreshToken(userId: Long, token: String, expiresAt: LocalDateTime) = newSuspendedTransaction {
        Users.update({ Users.id eq userId }) {
            it[refreshToken] = token
            it[refreshTokenExpiresAt] = expiresAt
            it[refreshTokenRevoked] = false
        }
    }

    suspend fun revokeRefreshToken(userId: Long) = newSuspendedTransaction {
        Users.update({ Users.id eq userId }) {
            it[refreshTokenRevoked] = true
        }
    }
}
