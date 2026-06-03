package dev.stoneworks.contextspace.tables

import dev.stoneworks.contextspace.util.DateTimeUtil
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object Users : Table("users") {
    val id = long("id").autoIncrement()
    val username = varchar("username", 64).uniqueIndex()
    val passwordHash = varchar("password_hash", 256)
    val refreshToken = varchar("refresh_token", 512).nullable()
    val refreshTokenExpiresAt = datetime("refresh_token_expires_at").nullable()
    val refreshTokenRevoked = bool("refresh_token_revoked").default(false)
    val createdAt = datetime("created_at").clientDefault { DateTimeUtil.now() }

    override val primaryKey = PrimaryKey(id)
}

data class UserRow(
    val id: Long,
    val username: String,
    val passwordHash: String,
    val refreshToken: String? = null,
    val refreshTokenExpiresAt: java.time.LocalDateTime? = null,
    val refreshTokenRevoked: Boolean = false,
)
