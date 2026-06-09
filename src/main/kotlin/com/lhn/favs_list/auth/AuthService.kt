package com.lhn.favs_list.auth

import com.lhn.favs_list.sessions.UserLoginSessionRepository
import com.lhn.favs_list.sessions.persistence.UserLoginSessionEntity
import com.lhn.favs_list.sessions.persistence.UserLoginSessionStatus
import com.lhn.favs_list.shared.ids.UuidGenerator
import com.lhn.favs_list.shared.logging.SecurityEventLogger
import com.lhn.favs_list.shared.validation.UserInputValidator
import com.lhn.favs_list.users.EmailAlreadyInUseException
import com.lhn.favs_list.users.UserProfile
import com.lhn.favs_list.users.UserRepository
import com.lhn.favs_list.users.persistence.UserEntity
import com.lhn.favs_list.users.toUserProfile
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Clock
import java.util.Base64
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val sessionRepository: UserLoginSessionRepository,
    private val userInputValidator: UserInputValidator,
    private val passwordHasher: PasswordHasher,
    private val tokenService: TokenService,
    private val uuidGenerator: UuidGenerator,
    private val clock: Clock,
    private val securityEventLogger: SecurityEventLogger,
) {

    @Transactional
    fun registerUser(command: RegisterUserCommand): UserProfile {
        val normalizedInput = userInputValidator.normalizeRegistrationInput(
            email = command.email,
            name = command.name,
            nickname = command.nickname,
            password = command.password,
        )

        if (userRepository.existsByNormalizedEmail(normalizedInput.email)) {
            securityEventLogger.registrationFailed(
                email = normalizedInput.email,
                reason = "DUPLICATE_EMAIL",
            )
            throw EmailAlreadyInUseException(normalizedInput.email)
        }

        val now = clock.instant()
        val user = UserEntity(
            id = uuidGenerator.randomUuid(),
            email = normalizedInput.email,
            name = normalizedInput.name,
            nickname = normalizedInput.nickname,
            passwordHash = passwordHasher.hash(normalizedInput.password),
            createdAt = now,
            updatedAt = now,
        )

        val savedUser = try {
            userRepository.save(user)
        } catch (exception: DataIntegrityViolationException) {
            val resolvedException = resolveRegistrationConflict(
                email = normalizedInput.email,
                cause = exception,
            )
            if (resolvedException is EmailAlreadyInUseException) {
                securityEventLogger.registrationFailed(
                    email = normalizedInput.email,
                    reason = "DUPLICATE_EMAIL",
                )
            }
            throw resolvedException
        }

        securityEventLogger.registrationSucceeded(
            userId = savedUser.id,
            email = savedUser.email,
        )

        return savedUser.toUserProfile()
    }

    @Transactional
    fun login(
        command: LoginCommand,
        requestMetadata: RequestMetadata = RequestMetadata(),
    ): LoginResult {
        val normalizedInput = userInputValidator.normalizeLoginInput(
            email = command.email,
            password = command.password,
        )
        val user = userRepository.findByNormalizedEmail(normalizedInput.email)

        if (user == null) {
            recordFailedLoginAttempt(
                attemptedEmail = normalizedInput.email,
                user = null,
                failureReason = LoginFailureReason.USER_NOT_FOUND,
                requestMetadata = requestMetadata,
            )
            throw AuthenticationFailedException()
        }

        if (user.deletedAt != null) {
            recordFailedLoginAttempt(
                attemptedEmail = normalizedInput.email,
                user = user,
                failureReason = LoginFailureReason.USER_DELETED,
                requestMetadata = requestMetadata,
            )
            throw AuthenticationFailedException()
        }

        if (!passwordHasher.matches(normalizedInput.password, user.passwordHash)) {
            recordFailedLoginAttempt(
                attemptedEmail = normalizedInput.email,
                user = user,
                failureReason = LoginFailureReason.INVALID_PASSWORD,
                requestMetadata = requestMetadata,
            )
            throw AuthenticationFailedException()
        }

        val tokenJti = uuidGenerator.randomUuid().toString()
        val issuedAccessToken = tokenService.createAccessToken(
            userId = user.id,
            tokenJti = tokenJti,
        )
        val session = UserLoginSessionEntity(
            id = uuidGenerator.randomUuid(),
            userId = user.id,
            attemptedEmail = normalizedInput.email,
            status = UserLoginSessionStatus.SUCCESS,
            tokenJti = issuedAccessToken.jti,
            tokenHash = hashToken(issuedAccessToken.token),
            issuedAt = issuedAccessToken.issuedAt,
            expiresAt = issuedAccessToken.expiresAt,
            ipAddress = requestMetadata.ipAddress,
            userAgent = requestMetadata.userAgent,
            createdAt = issuedAccessToken.issuedAt,
            updatedAt = issuedAccessToken.issuedAt,
        )
        sessionRepository.saveSuccessfulSession(session)
        securityEventLogger.loginSucceeded(
            userId = user.id,
            sessionId = session.id,
            tokenJti = issuedAccessToken.jti,
            requestMetadata = requestMetadata,
        )

        return LoginResult(
            accessToken = issuedAccessToken.token,
            tokenType = "Bearer",
            expiresIn = issuedAccessToken.expiresInSeconds,
            user = user.toUserProfile(),
        )
    }

    @Transactional
    fun logout(
        userId: java.util.UUID,
        currentSessionJti: String,
    ): Boolean {
        val now = clock.instant()
        val revoked = sessionRepository.revokeByJti(
            tokenJti = currentSessionJti,
            revokedAt = now,
            activeAt = now,
        )
        if (revoked) {
            securityEventLogger.logoutSucceeded(
                userId = userId,
                sessionJti = currentSessionJti,
            )
        }
        return revoked
    }

    private fun resolveRegistrationConflict(
        email: String,
        cause: DataIntegrityViolationException,
    ): RuntimeException {
        if (userRepository.existsByNormalizedEmail(email)) {
            return EmailAlreadyInUseException(email, cause)
        }

        return cause
    }

    private fun recordFailedLoginAttempt(
        attemptedEmail: String,
        user: UserEntity?,
        failureReason: LoginFailureReason,
        requestMetadata: RequestMetadata,
    ) {
        val now = clock.instant()
        val failedAttempt = UserLoginSessionEntity(
            id = uuidGenerator.randomUuid(),
            userId = user?.id,
            attemptedEmail = attemptedEmail,
            status = UserLoginSessionStatus.FAILED,
            failureReason = failureReason.name,
            ipAddress = requestMetadata.ipAddress,
            userAgent = requestMetadata.userAgent,
            createdAt = now,
            updatedAt = now,
        )

        sessionRepository.saveFailedLoginAttempt(failedAttempt)
        securityEventLogger.loginFailed(
            attemptedEmail = attemptedEmail,
            userId = user?.id,
            reason = failureReason,
            requestMetadata = requestMetadata,
        )
    }

    private fun hashToken(token: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(token.toByteArray(StandardCharsets.UTF_8)),
        )
}

data class RegisterUserCommand(
    val email: String,
    val name: String,
    val nickname: String,
    val password: String,
)

data class LoginCommand(
    val email: String,
    val password: String,
)

data class LoginResult(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
    val user: UserProfile,
)

data class RequestMetadata(
    val requestId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
)

enum class LoginFailureReason {
    INVALID_PASSWORD,
    USER_NOT_FOUND,
    USER_DELETED,
}

class AuthenticationFailedException :
    RuntimeException("Invalid email or password")
