package dev.stoneworks.contextspace

import dev.stoneworks.common.util.logger
import io.ktor.server.config.*
import io.lettuce.core.*
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import kotlin.time.measureTime

class RedisConnection internal constructor(
    private val commands: RedisClusterCommands<String, String>,
) {
    fun sync(): RedisClusterCommands<String, String> = commands
}

object RedisConfig {

    private var client: AbstractRedisClient? = null
    private var writeConn: AutoCloseable? = null
    private var readConn: AutoCloseable? = null
    private var writeRedisConn: RedisConnection? = null
    private var readRedisConn: RedisConnection? = null

    private val log = logger(this)

    fun init(config: ApplicationConfig) {
        measureTime {
            val redisConfig = config.config("redis")
            val uriStr = redisConfig.property("uri").getString()
            val isCluster = try {
                redisConfig.property("isCluster").getString().toBoolean()
            } catch (_: Exception) {
                false
            }

            if (isCluster) {
                val clusterClient = RedisClusterClient.create(uriStr)
                client = clusterClient

                val wConn = clusterClient.connect()
                wConn.readFrom = ReadFrom.MASTER
                writeConn = wConn
                writeRedisConn = RedisConnection(wConn.sync())

                val rConn = clusterClient.connect()
                rConn.readFrom = ReadFrom.REPLICA_PREFERRED
                readConn = rConn
                readRedisConn = RedisConnection(rConn.sync())
            } else {
                val baseUri = RedisURI.create(uriStr)
                val standaloneClient = RedisClient.create(baseUri)
                client = standaloneClient

                val primary = standaloneClient.connect()
                writeConn = primary
                writeRedisConn = RedisConnection(primary.sync())
                readConn = primary
                readRedisConn = writeRedisConn
            }
        }.let { duration -> log.info { "init done in ${duration.inWholeMilliseconds}ms" } }
    }

    fun writeConnection(): RedisConnection? = writeRedisConn

    fun readConnection(): RedisConnection? = readRedisConn

    fun close() {
        writeConn?.close()
        if (readConn != null && readConn != writeConn) readConn?.close()
        client?.shutdown()
    }
}
