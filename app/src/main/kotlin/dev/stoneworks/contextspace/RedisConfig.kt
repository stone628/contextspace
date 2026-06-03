package dev.stoneworks.contextspace

import io.ktor.server.config.ApplicationConfig
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection

object RedisConfig {

    private var client: RedisClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null

    fun init(config: ApplicationConfig) {
        val uri = config.config("redis").property("uri").getString()
        client = RedisClient.create(uri)
        connection = client?.connect()
    }

    fun connection(): StatefulRedisConnection<String, String>? = connection

    fun close() {
        connection?.close()
        client?.shutdown()
    }
}
