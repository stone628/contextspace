package dev.stoneworks.contextspace.tables

import dev.stoneworks.common.util.DateTimeUtil
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.json.jsonb

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 64).uniqueIndex()
    val content = jsonb<UsersContent>("content", Json.Default)
        .clientDefault { UsersContent(passwordHash = "") }
    val createdAt = datetime("created_at").clientDefault { DateTimeUtil.now() }

    override val primaryKey = PrimaryKey(id)
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
)

@Serializable
data class UsersContent(
    val passwordHash: String,
    val nickname: String = "",
    val refreshToken: String? = null,
    val refreshTokenExpiresAt: Long? = null,
    val refreshTokenRevoked: Boolean = false,
)
