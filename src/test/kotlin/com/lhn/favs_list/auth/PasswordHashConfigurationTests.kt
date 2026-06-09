package com.lhn.favs_list.auth

import com.lhn.favs_list.shared.config.PasswordHashAlgorithm
import com.lhn.favs_list.shared.config.PasswordHashProperties
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordHashConfigurationTests {

    private val configuration = PasswordHashConfiguration()

    @Test
    fun `argon2id password hasher hashes and verifies passwords`() {
        val passwordHasher = configuration.passwordHasher(
            PasswordHashProperties(algorithm = PasswordHashAlgorithm.ARGON2ID),
        )

        val firstPasswordHash = passwordHasher.hash("Secret123")
        val secondPasswordHash = passwordHasher.hash("Secret123")

        assertNotEquals("Secret123", firstPasswordHash)
        assertNotEquals(firstPasswordHash, secondPasswordHash)
        assertTrue(passwordHasher.matches("Secret123", firstPasswordHash))
        assertTrue(passwordHasher.matches("Secret123", secondPasswordHash))
        assertFalse(passwordHasher.matches("Other123", firstPasswordHash))
    }

    @Test
    fun `bcrypt password hasher hashes and verifies passwords`() {
        val passwordHasher = configuration.passwordHasher(
            PasswordHashProperties(algorithm = PasswordHashAlgorithm.BCRYPT),
        )

        val firstPasswordHash = passwordHasher.hash("Secret123")
        val secondPasswordHash = passwordHasher.hash("Secret123")

        assertNotEquals("Secret123", firstPasswordHash)
        assertNotEquals(firstPasswordHash, secondPasswordHash)
        assertTrue(passwordHasher.matches("Secret123", firstPasswordHash))
        assertTrue(passwordHasher.matches("Secret123", secondPasswordHash))
        assertFalse(passwordHasher.matches("Other123", firstPasswordHash))
    }
}
