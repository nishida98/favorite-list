package com.lhn.favs_list.shared.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserInputValidatorTests {

    private val validator = UserInputValidator()

    @Test
    fun `normalizes registration fields before persistence checks`() {
        val normalized = validator.normalizeRegistrationInput(
            email = "  Test.User@Example.COM ",
            name = "  Test User  ",
            nickname = "  Test.User  ",
            password = "Secret123",
        )

        assertEquals("test.user@example.com", normalized.email)
        assertEquals("Test User", normalized.name)
        assertEquals("Test.User", normalized.nickname)
        assertEquals("Secret123", normalized.password)
    }

    @Test
    fun `collects field validation errors for invalid registration input`() {
        val exception = assertFailsWith<InvalidInputException> {
            validator.normalizeRegistrationInput(
                email = "bad",
                name = " ",
                nickname = "a",
                password = "short",
            )
        }

        assertEquals(
            listOf("email", "name", "nickname", "password"),
            exception.fieldErrors.map(FieldValidationError::field),
        )
    }

    @Test
    fun `requires at least one editable profile field`() {
        val exception = assertFailsWith<InvalidInputException> {
            validator.normalizeProfileUpdateInput(
                name = null,
                nickname = null,
            )
        }

        assertEquals("input", exception.fieldErrors.single().field)
    }
}
