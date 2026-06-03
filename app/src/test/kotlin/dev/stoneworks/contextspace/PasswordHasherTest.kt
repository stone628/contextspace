package dev.stoneworks.contextspace

import dev.stoneworks.contextspace.auth.PasswordHasher
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordHasherTest {

    @Test
    fun `hash and verify matching password`() {
        val password = "mySecretPassword123"
        val hash = PasswordHasher.hash(password)
        assertTrue(PasswordHasher.verify(password, hash))
    }

    @Test
    fun `verify wrong password`() {
        val password = "correctPassword"
        val hash = PasswordHasher.hash(password)
        assertFalse(PasswordHasher.verify("wrongPassword", hash))
    }
}
