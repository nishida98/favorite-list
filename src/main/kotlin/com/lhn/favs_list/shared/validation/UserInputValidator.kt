package com.lhn.favs_list.shared.validation

import org.springframework.stereotype.Component

@Component
class UserInputValidator {

    fun normalizeRegistrationInput(
        email: String,
        name: String,
        nickname: String,
        password: String,
    ): NormalizedRegistrationInput {
        val fieldErrors = mutableListOf<FieldValidationError>()

        val normalizedEmail = collect(fieldErrors) { normalizeEmail(email) }
        val normalizedName = collect(fieldErrors) { normalizeName(name) }
        val normalizedNickname = collect(fieldErrors) { normalizeNickname(nickname) }
        val normalizedPassword = collect(fieldErrors) { validatePassword(password) }

        ensureNoErrors(fieldErrors)

        return NormalizedRegistrationInput(
            email = normalizedEmail!!,
            name = normalizedName!!,
            nickname = normalizedNickname!!,
            password = normalizedPassword!!,
        )
    }

    fun normalizeLoginInput(
        email: String,
        password: String,
    ): NormalizedLoginInput {
        val fieldErrors = mutableListOf<FieldValidationError>()

        val normalizedEmail = collect(fieldErrors) { normalizeEmail(email) }
        val validatedPassword = collect(fieldErrors) { validatePassword(password) }

        ensureNoErrors(fieldErrors)

        return NormalizedLoginInput(
            email = normalizedEmail!!,
            password = validatedPassword!!,
        )
    }

    fun normalizeProfileUpdateInput(
        name: String?,
        nickname: String?,
    ): NormalizedProfileUpdateInput {
        val fieldErrors = mutableListOf<FieldValidationError>()

        if (name == null && nickname == null) {
            fieldErrors += FieldValidationError(
                field = "input",
                message = "At least one editable field must be provided",
            )
        }

        val normalizedName = name?.let { collect(fieldErrors) { normalizeName(it) } }
        val normalizedNickname = nickname?.let { collect(fieldErrors) { normalizeNickname(it) } }

        ensureNoErrors(fieldErrors)

        return NormalizedProfileUpdateInput(
            name = normalizedName,
            nickname = normalizedNickname,
        )
    }

    fun normalizeEmail(rawEmail: String): String {
        val trimmedEmail = rawEmail.trim()

        if (trimmedEmail.isEmpty()) {
            throw invalidField("email", "Email is required")
        }
        if (trimmedEmail.length > MAX_EMAIL_LENGTH) {
            throw invalidField("email", "Email must be at most $MAX_EMAIL_LENGTH characters")
        }

        val normalizedEmail = trimmedEmail.lowercase()
        if (!EMAIL_REGEX.matches(normalizedEmail)) {
            throw invalidField("email", "Email format is invalid")
        }

        return normalizedEmail
    }

    fun normalizeName(rawName: String): String {
        val trimmedName = rawName.trim()

        if (trimmedName.isEmpty()) {
            throw invalidField("name", "Name is required")
        }
        if (trimmedName.length < MIN_NAME_LENGTH) {
            throw invalidField("name", "Name must be at least $MIN_NAME_LENGTH characters")
        }
        if (trimmedName.length > MAX_NAME_LENGTH) {
            throw invalidField("name", "Name must be at most $MAX_NAME_LENGTH characters")
        }

        return trimmedName
    }

    fun normalizeNickname(rawNickname: String): String {
        val trimmedNickname = rawNickname.trim()

        if (trimmedNickname.isEmpty()) {
            throw invalidField("nickname", "Nickname is required")
        }
        if (trimmedNickname.length < MIN_NICKNAME_LENGTH) {
            throw invalidField("nickname", "Nickname must be at least $MIN_NICKNAME_LENGTH characters")
        }
        if (trimmedNickname.length > MAX_NICKNAME_LENGTH) {
            throw invalidField("nickname", "Nickname must be at most $MAX_NICKNAME_LENGTH characters")
        }
        if (!NICKNAME_REGEX.matches(trimmedNickname)) {
            throw invalidField(
                "nickname",
                "Nickname may contain only letters, numbers, underscore, hyphen, and dot",
            )
        }

        return trimmedNickname
    }

    fun validatePassword(rawPassword: String): String {
        if (rawPassword.isBlank()) {
            throw invalidField("password", "Password is required")
        }
        if (rawPassword.length < MIN_PASSWORD_LENGTH) {
            throw invalidField("password", "Password must be at least $MIN_PASSWORD_LENGTH characters")
        }
        if (rawPassword.length > MAX_PASSWORD_LENGTH) {
            throw invalidField("password", "Password must be at most $MAX_PASSWORD_LENGTH characters")
        }
        if (!rawPassword.any(Char::isLetter)) {
            throw invalidField("password", "Password must contain at least one letter")
        }
        if (!rawPassword.any(Char::isDigit)) {
            throw invalidField("password", "Password must contain at least one number")
        }

        return rawPassword
    }

    private fun invalidField(field: String, message: String): InvalidInputException =
        InvalidInputException(listOf(FieldValidationError(field = field, message = message)))

    private fun ensureNoErrors(fieldErrors: List<FieldValidationError>) {
        if (fieldErrors.isNotEmpty()) {
            throw InvalidInputException(fieldErrors)
        }
    }

    private fun <T> collect(
        fieldErrors: MutableList<FieldValidationError>,
        block: () -> T,
    ): T? =
        try {
            block()
        } catch (exception: InvalidInputException) {
            fieldErrors += exception.fieldErrors
            null
        }

    companion object {
        private const val MAX_EMAIL_LENGTH = 255
        private const val MIN_NAME_LENGTH = 2
        private const val MAX_NAME_LENGTH = 255
        private const val MIN_NICKNAME_LENGTH = 3
        private const val MAX_NICKNAME_LENGTH = 80
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MAX_PASSWORD_LENGTH = 128

        private val EMAIL_REGEX =
            Regex("^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,63}$")

        private val NICKNAME_REGEX =
            Regex("^[a-zA-Z0-9_.-]{3,80}$")
    }
}

data class NormalizedRegistrationInput(
    val email: String,
    val name: String,
    val nickname: String,
    val password: String,
)

data class NormalizedLoginInput(
    val email: String,
    val password: String,
)

data class NormalizedProfileUpdateInput(
    val name: String?,
    val nickname: String?,
)
