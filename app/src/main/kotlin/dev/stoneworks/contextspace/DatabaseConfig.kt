package dev.stoneworks.contextspace

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.config.ApplicationConfig
import org.jetbrains.exposed.v1.jdbc.Database

object DatabaseConfig {

    private var dataSource: HikariDataSource? = null

    fun init(config: ApplicationConfig) {
        val dbConfig = config.config("database")
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = dbConfig.property("url").getString()
            driverClassName = dbConfig.property("driver").getString()
            username = dbConfig.property("user").getString()
            password = dbConfig.property("password").getString()
            maximumPoolSize = dbConfig.property("poolSize").getString().toInt()
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val ds = HikariDataSource(hikariConfig)
        dataSource = ds
        Database.connect(ds)
    }

    fun close() {
        dataSource?.close()
    }
}
