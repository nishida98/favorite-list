package com.lhn.favs_list.users

import com.lhn.favs_list.sessions.UserLoginSessionRepository
import com.lhn.favs_list.shared.logging.SecurityEventLogger
import com.lhn.favs_list.shared.validation.UserInputValidator
import java.time.Clock
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val sessionRepository: UserLoginSessionRepository,
    private val userInputValidator: UserInputValidator,
    private val clock: Clock,
    private val securityEventLogger: SecurityEventLogger,
) {

    @Transactional(readOnly = true)
    fun getCurrentUser(userId: UUID): UserProfile =
        userRepository.findActiveById(userId)?.toUserProfile()
            ?: throw CurrentUserNotFoundException(userId)

    @Transactional
    fun updateCurrentUser(
        userId: UUID,
        command: UpdateCurrentUserCommand,
    ): UserProfile {
        val normalizedInput = userInputValidator.normalizeProfileUpdateInput(
            name = command.name,
            nickname = command.nickname,
        )
        val user = userRepository.findActiveById(userId)
            ?: throw CurrentUserNotFoundException(userId)

        normalizedInput.name?.let { user.name = it }
        normalizedInput.nickname?.let { user.nickname = it }
        user.updatedAt = clock.instant()

        val savedUser = userRepository.update(user)

        return savedUser.toUserProfile()
    }

    @Transactional
    fun deleteCurrentUser(userId: UUID): Boolean {
        val user = userRepository.findActiveById(userId)
            ?: throw CurrentUserNotFoundException(userId)
        val now = clock.instant()

        user.deletedAt = now
        user.updatedAt = now
        userRepository.update(user)
        val revokedSessionCount = sessionRepository.revokeActiveSessionsByUserId(
            userId = userId,
            revokedAt = now,
            activeAt = now,
        )
        securityEventLogger.userSoftDeleted(userId)
        securityEventLogger.sessionRevocationSucceeded(
            userId = userId,
            revokedSessionCount = revokedSessionCount,
            trigger = "USER_SOFT_DELETE",
        )

        return true
    }
}

data class UpdateCurrentUserCommand(
    val name: String? = null,
    val nickname: String? = null,
)

class CurrentUserNotFoundException(userId: UUID) :
    RuntimeException("Active user was not found for id=$userId")
