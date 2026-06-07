package dev.stoneworks.contextspace

import dev.stoneworks.common.util.StringUtil
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordHasherTest {

    @Test
    fun `hash and verify matching password`() {
        val password = "mySecretPassword123"
        val hash = StringUtil.hashPassword(password)
        assertTrue(StringUtil.verifyPassword(password, hash))
    }

    @Test
    fun `verify wrong password`() {
        val password = "correctPassword"
        val hash = StringUtil.hashPassword(password)
        assertFalse(StringUtil.verifyPassword("wrongPassword", hash))
    }
}
