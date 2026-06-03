package dev.stoneworks.contextspace.tables

import dev.stoneworks.contextspace.util.DateTimeUtil
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime

object RefreshTokens : Table("refresh_tokens") {
    val id = long("id").autoIncrement()
    val token = varchar("token", 512).uniqueIndex()
    val userId = long("user_id").references(Users.id)
    val expiresAt = datetime("expires_at")
    val revoked = bool("revoked").default(false)
    val createdAt = datetime("created_at").clientDefault { DateTimeUtil.now() }

    override val primaryKey = PrimaryKey(id)
}

data class RefreshTokenRow(
    val id: Long,
    val token: String,
    val userId: Long,
    val expiresAt: java.time.LocalDateTime,
    val revoked: Boolean,
)
