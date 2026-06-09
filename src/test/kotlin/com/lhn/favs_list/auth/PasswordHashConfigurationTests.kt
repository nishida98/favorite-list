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

        val passwordHash = passwordHasher.hash("Secret123")

        assertNotEquals("Secret123", passwordHash)
        assertTrue(passwordHasher.matches("Secret123", passwordHash))
        assertFalse(passwordHasher.matches("Other123", passwordHash))
    }

    @Test
    fun `bcrypt password hasher hashes and verifies passwords`() {
        val passwordHasher = configuration.passwordHasher(
            PasswordHashProperties(algorithm = PasswordHashAlgorithm.BCRYPT),
        )

        val passwordHash = passwordHasher.hash("Secret123")

        assertNotEquals("Secret123", passwordHash)
        assertTrue(passwordHasher.matches("Secret123", passwordHash))
        assertFalse(passwordHasher.matches("Other123", passwordHash))
    }
}
