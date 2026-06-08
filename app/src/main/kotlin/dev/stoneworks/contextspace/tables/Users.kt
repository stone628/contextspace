package dev.stoneworks.contextspace.tables

import dev.stoneworks.common.component.jsonContent
import dev.stoneworks.common.registerTable
import dev.stoneworks.common.util.DateTimeUtil
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.javatime.datetime

object Users : Table("users") {
    init {
        registerTable(this)
    }

    val id = long("id").autoIncrement()
    val username = varchar("username", 64).uniqueIndex()
    val content = jsonContent<UsersContent>("content").clientDefault { UsersContent(passwordHash = "") }
    val createdAt = datetime("created_at").clientDefault { DateTimeUtil.now() }

    override val primaryKey = PrimaryKey(id)

    fun toRow(it: ResultRow): UserRow {
        val c = it[content]

        return UserRow(
            id = it[id],
            username = it[username],
            passwordHash = c.passwordHash,
            nickname = c.nickname.ifBlank { it[username] },
            createdAt = it[createdAt],
            refreshToken = c.refreshToken,
            refreshTokenExpiresAt = c.refreshTokenExpiresAt?.let(DateTimeUtil::fromEpochMillis),
            refreshTokenRevoked = c.refreshTokenRevoked
        )
    }
}

data class UserRow(
    val id: Long,
    val username: String,
    val passwordHash: String,
    val nickname: String,
    val createdAt: java.time.LocalDateTime,
    val refreshToken: String? = null,
    val refreshTokenExpiresAt: java.time.LocalDateTime? = null,
    val refreshTokenRevoked: Boolean = false,
) {
    fun toContent() = UsersContent(
        passwordHash = passwordHash,
        nickname = nickname,
        refreshToken = refreshToken,
        refreshTokenExpiresAt = refreshTokenExpiresAt?.let(DateTimeUtil::toEpochMillis),
        refreshTokenRevoked = refreshTokenRevoked,
    )
}

@Serializable
data class UsersContent(
    val passwordHash: String,
    val nickname: String = "",
    val refreshToken: String? = null,
    val refreshTokenExpiresAt: Long? = null,
    val refreshTokenRevoked: Boolean = false,
)
