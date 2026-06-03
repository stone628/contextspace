package dev.stoneworks.contextspace.dao

import dev.stoneworks.contextspace.tables.UserRow
import dev.stoneworks.contextspace.tables.Users
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

object UserDao {

    suspend fun findByUsername(username: String): UserRow? = newSuspendedTransaction {
        Users.selectAll().where(Users.username eq username).singleOrNull()?.let {
            UserRow(
                id = it[Users.id],
                username = it[Users.username],
                passwordHash = it[Users.passwordHash],
            )
        }
    }

    suspend fun create(username: String, passwordHash: String): UserRow = newSuspendedTransaction {
        val insert = Users.insert {
            it[Users.username] = username
            it[Users.passwordHash] = passwordHash
        }
        UserRow(id = insert[Users.id], username = username, passwordHash = passwordHash)
    }

    suspend fun findById(id: Long): UserRow? = newSuspendedTransaction {
        Users.selectAll().where(Users.id eq id).singleOrNull()?.let {
            UserRow(
                id = it[Users.id],
                username = it[Users.username],
                passwordHash = it[Users.passwordHash],
            )
        }
    }
}
