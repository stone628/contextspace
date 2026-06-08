package dev.stoneworks.common.component

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.stoneworks.common.registerInit
import dev.stoneworks.common.registerShutdown
import dev.stoneworks.common.util.logger
import io.ktor.server.config.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.json.jsonb
import kotlin.time.measureTime

object DatabaseConfig {
    init {
        registerInit { config -> init(config) }
        registerShutdown { close() }
    }

    private val log = logger(this)

    private var dataSource: HikariDataSource? = null

    fun init(config: ApplicationConfig) {
        measureTime {
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
        }.let { duration -> log.info { "init done in ${duration.inWholeMilliseconds}ms" } }
    }

    fun close() {
        dataSource?.close()
    }

    val defaultJsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
}

inline fun <reified T : Any> Table.jsonContent(
    name: String,
): Column<T> {
    val kSerializer = serializer<T>()
    val jc = DatabaseConfig.defaultJsonConfig

    return jsonb(
        name,
        { jc.encodeToString(kSerializer, it) },
        { jc.decodeFromString(kSerializer, it) },
        false
    )
}
