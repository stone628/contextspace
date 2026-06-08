package dev.stoneworks.common.component

import dev.stoneworks.common.registerInit
import dev.stoneworks.common.registerShutdown
import dev.stoneworks.common.util.logger
import io.ktor.server.config.*
import io.lettuce.core.*
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlin.time.measureTime

class RedisConnection internal constructor(
    private val commands: RedisClusterCommands<String, String>,
) {
    fun sync(): RedisClusterCommands<String, String> = commands
}

object RedisConfig {
    init {
        registerInit { config -> init(config) }
        registerShutdown { close() }
    }

    private val log = logger(this)

    private var client: AbstractRedisClient? = null
    private var writeConn: AutoCloseable? = null
    private var readConn: AutoCloseable? = null
    private var writeRedisConn: RedisConnection? = null
    private var readRedisConn: RedisConnection? = null

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

                val (wConn, rConn) = runBlocking {
                    coroutineScope {
                        async(Dispatchers.IO) {
                            val c = clusterClient.connect()
                            c.readFrom = ReadFrom.MASTER
                            c
                        } to async(Dispatchers.IO) {
                            val c = clusterClient.connect()
                            c.readFrom = ReadFrom.REPLICA_PREFERRED
                            c
                        }
                    }.let { (w, r) -> w.await() to r.await() }
                }
                writeConn = wConn
                writeRedisConn = RedisConnection(wConn.sync())
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
