package dev.stoneworks.contextspace.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import dev.stoneworks.contextspace.util.DateTimeUtil
import io.ktor.server.config.*
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

object JwtUtils {

    private lateinit var authAlgorithm: Algorithm
    private lateinit var refreshAlgorithm: Algorithm
    private lateinit var authIssuer: String
    private lateinit var refreshIssuer: String
    private var authExpiresInMs: Long = 0
    private var refreshExpiresInMs: Long = 0

    fun init(config: ApplicationConfig) {
        val jwtConfig = config.config("jwt")
        val authSecret = jwtConfig.property("authSecret").getString()
        val refreshSecret = jwtConfig.property("refreshSecret").getString()
        authIssuer = jwtConfig.property("authIssuer").getString()
        refreshIssuer = jwtConfig.property("refreshIssuer").getString()
        authExpiresInMs = jwtConfig.property("authExpiresInMinutes").getString().toLong() * 60 * 1000
        refreshExpiresInMs = jwtConfig.property("refreshExpiresInDays").getString().toLong() * 24 * 60 * 60 * 1000
        authAlgorithm = Algorithm.HMAC256(authSecret)
        refreshAlgorithm = Algorithm.HMAC256(refreshSecret)
    }

    fun generateAuthToken(userId: Long, username: String, now: LocalDateTime): String {
        return JWT.create()
            .withJWTId(UUID.randomUUID().toString())
            .withIssuer(authIssuer)
            .withSubject(userId.toString())
            .withClaim("username", username)
            .withIssuedAt(DateTimeUtil.toInstant(now))
            .withExpiresAt(DateTimeUtil.toDate(now.plus(authExpiresInMs, ChronoUnit.MILLIS)))
            .sign(authAlgorithm)
    }

    fun generateRefreshToken(userId: Long, now: LocalDateTime): String {
        return JWT.create()
            .withJWTId(UUID.randomUUID().toString())
            .withIssuer(refreshIssuer)
            .withSubject(userId.toString())
            .withIssuedAt(DateTimeUtil.toInstant(now))
            .withExpiresAt(DateTimeUtil.toDate(now.plus(refreshExpiresInMs, ChronoUnit.MILLIS)))
            .sign(refreshAlgorithm)
    }

    fun verifyAuthToken(token: String): DecodedJWT? {
        return try {
            JWT.require(authAlgorithm)
                .withIssuer(authIssuer)
                .build()
                .verify(token)
        } catch (_: Exception) {
            null
        }
    }

    fun verifyRefreshToken(token: String): DecodedJWT? {
        return try {
            JWT.require(refreshAlgorithm)
                .withIssuer(refreshIssuer)
                .build()
                .verify(token)
        } catch (_: Exception) {
            null
        }
    }

    fun isExpired(jwt: DecodedJWT, now: LocalDateTime): Boolean {
        return jwt.expiresAt.before(DateTimeUtil.toDate(now))
    }
}
