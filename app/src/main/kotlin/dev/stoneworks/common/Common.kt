package dev.stoneworks.common

import dev.stoneworks.common.util.fromConfig
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction

interface Registered {
    fun inits(): List<(ApplicationConfig) ->Unit>
    fun shutdowns(): List<() -> Unit>
    fun tables(): List<Table>
    fun routes(): List<Route.() -> Unit>
}

fun registerInit(i: (ApplicationConfig) -> Unit) = ModuleRegistry.init(i)
fun registerShutdown(s: () -> Unit) = ModuleRegistry.shutdown(s)
fun registerRoute(r : Route.() -> Unit) = ModuleRegistry.registerRoute(r)
fun registerTable(t : Table) = ModuleRegistry.table(t)

fun registered(): Registered = ModuleRegistry

private object ModuleRegistry: Registered {
    private val inits = mutableListOf<(ApplicationConfig) -> Unit>()
    private val shutdowns = mutableListOf<() -> Unit>()
    private val tables = mutableListOf<Table>()
    private val routes = mutableListOf< Route.() -> Unit>()

    fun init(init: (ApplicationConfig) -> Unit) {
        inits.add(init)
    }

    fun shutdown(shutdown: () -> Unit) {
        shutdowns.add(shutdown)
    }

    fun table(t : Table) {
        tables.add(t)
    }

    fun registerRoute(r: Route.() -> Unit) {
        routes.add(r)
    }

    override fun inits(): List<(ApplicationConfig) ->Unit> = inits
    override fun shutdowns(): List<() -> Unit> = shutdowns.reversed()
    override fun tables(): List<Table> = tables
    override fun routes(): List<Route.() -> Unit> = routes
}

fun Application.common() {
    preload()

    val config = environment.config
    val registered = registered()

    for (i in registered.inits()) {
        i(config)
    }

    monitor.subscribe(ApplicationStopping) {
        for (s in registered.shutdowns()) {
            s()
        }
    }

    runBlocking {
        val tables = registered.tables()

        if (tables.isNotEmpty()) {
            suspendTransaction {
                SchemaUtils.create(tables = tables.toTypedArray(), inBatch = true)
            }
        }
    }

    install(CORS) {
        fromConfig(config)
    }

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
        })
    }

    routing {
        for (r in registered.routes()) {
            r(this)
        }
    }
}