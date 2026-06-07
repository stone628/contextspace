package dev.stoneworks.contextspace

import dev.stoneworks.common.component.JwtUtils
import dev.stoneworks.common.util.DateTimeUtil
import io.ktor.server.config.MapApplicationConfig
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JwtUtilsTest {

    @BeforeTest
    fun setup() {
        val config = MapApplicationConfig(
            "jwt.authSecret" to "test-auth-secret",
            "jwt.authIssuer" to "test-issuer",
            "jwt.authExpiresInMinutes" to "5",
            "jwt.refreshSecret" to "test-refresh-secret",
            "jwt.refreshIssuer" to "test-issuer",
            "jwt.refreshExpiresInDays" to "7",
        )
        JwtUtils.init(config)
    }

    @Test
    fun `generate and verify auth token`() {
        val now = DateTimeUtil.now()
        val token = JwtUtils.generateAuthToken(1, "testuser", now)
        val decoded = JwtUtils.verifyAuthToken(token)
        assertNotNull(decoded)
        assert(!JwtUtils.isExpired(decoded, now))
    }

    @Test
    fun `reject invalid auth token`() {
        val result = JwtUtils.verifyAuthToken("invalid.token.here")
        assertNull(result)
    }

    @Test
    fun `generate and verify refresh token`() {
        val now = DateTimeUtil.now()
        val token = JwtUtils.generateRefreshToken(1, now)
        val decoded = JwtUtils.verifyRefreshToken(token)
        assertNotNull(decoded)
        assert(!JwtUtils.isExpired(decoded, now))
    }

    @Test
    fun `auth token contains correct subject`() {
        val token = JwtUtils.generateAuthToken(42, "alice", DateTimeUtil.now())
        val decoded = JwtUtils.verifyAuthToken(token)
        assertNotNull(decoded)
        assert(decoded.subject == "42")
    }
}
