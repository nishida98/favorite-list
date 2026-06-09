package com.lhn.favs_list.sessions.persistence

import com.lhn.favs_list.sessions.UserLoginSessionRepository
import com.lhn.favs_list.shared.testing.PostgreSqlRepositoryTestSupport
import com.lhn.favs_list.users.persistence.UserEntity
import jakarta.persistence.EntityManager
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.auth.jwt.secret=test-secret-value-12345678901234567890",
        "app.cors.allowed-origins[0]=https://app.example.test",
    ],
)
@Transactional
class JpaUserLoginSessionRepositoryTests : PostgreSqlRepositoryTestSupport() {

    @Autowired
    private lateinit var sessionRepository: UserLoginSessionRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `persists successful and failed login records`() {
        val user = persistUser()
        val successfulSession = successfulSession(userId = user.id, tokenJti = "session-success")
        val failedAttempt = failedAttempt(attemptedEmail = "missing@example.com", reason = "USER_NOT_FOUND")

        sessionRepository.saveSuccessfulSession(successfulSession)
        sessionRepository.saveFailedLoginAttempt(failedAttempt)
        entityManager.clear()

        val foundSuccessfulSession = sessionRepository.findByJti("session-success")

        assertNotNull(foundSuccessfulSession)
        assertEquals(UserLoginSessionStatus.SUCCESS, foundSuccessfulSession.status)
        assertEquals(user.id, foundSuccessfulSession.userId)
        assertNull(sessionRepository.findByJti("missing-jti"))
    }

    @Test
    fun `revokes only active sessions by jti`() {
        persistUser()
        sessionRepository.saveSuccessfulSession(
            successfulSession(
                tokenJti = "active-token",
                expiresAt = Instant.parse("2026-06-08T13:00:00Z"),
            ),
        )
        sessionRepository.saveSuccessfulSession(
            successfulSession(
                tokenJti = "expired-token",
                expiresAt = Instant.parse("2026-06-08T11:59:59Z"),
            ),
        )

        assertTrue(
            sessionRepository.revokeByJti(
                tokenJti = "active-token",
                revokedAt = Instant.parse("2026-06-08T12:30:00Z"),
                activeAt = Instant.parse("2026-06-08T12:30:00Z"),
            ),
        )
        assertFalse(
            sessionRepository.revokeByJti(
                tokenJti = "expired-token",
                revokedAt = Instant.parse("2026-06-08T12:30:00Z"),
                activeAt = Instant.parse("2026-06-08T12:30:00Z"),
            ),
        )

        val revokedSession = sessionRepository.findByJti("active-token")
        val expiredSession = sessionRepository.findByJti("expired-token")

        assertNotNull(revokedSession)
        assertEquals(UserLoginSessionStatus.REVOKED, revokedSession.status)
        assertEquals(Instant.parse("2026-06-08T12:30:00Z"), revokedSession.revokedAt)
        assertNotNull(expiredSession)
        assertEquals(UserLoginSessionStatus.SUCCESS, expiredSession.status)
        assertNull(expiredSession.revokedAt)
    }

    @Test
    fun `revokes only active sessions for a user`() {
        val user = persistUser()
        val otherUser = persistUser(
            email = "other@example.com",
            nickname = "other-user",
        )

        sessionRepository.saveSuccessfulSession(
            successfulSession(
                userId = user.id,
                tokenJti = "user-active",
                expiresAt = Instant.parse("2026-06-08T13:00:00Z"),
            ),
        )
        sessionRepository.saveSuccessfulSession(
            successfulSession(
                userId = user.id,
                tokenJti = "user-expired",
                expiresAt = Instant.parse("2026-06-08T11:59:59Z"),
            ),
        )
        sessionRepository.saveFailedLoginAttempt(
            failedAttempt(
                attemptedEmail = user.email,
                reason = "INVALID_PASSWORD",
                userId = user.id,
            ),
        )
        sessionRepository.saveSuccessfulSession(
            successfulSession(
                userId = otherUser.id,
                tokenJti = "other-active",
                expiresAt = Instant.parse("2026-06-08T13:00:00Z"),
            ),
        )

        val revokedCount = sessionRepository.revokeActiveSessionsByUserId(
            userId = user.id,
            revokedAt = Instant.parse("2026-06-08T12:45:00Z"),
            activeAt = Instant.parse("2026-06-08T12:45:00Z"),
        )

        assertEquals(1, revokedCount)
        assertEquals(UserLoginSessionStatus.REVOKED, sessionRepository.findByJti("user-active")?.status)
        assertEquals(UserLoginSessionStatus.SUCCESS, sessionRepository.findByJti("user-expired")?.status)
        assertEquals(UserLoginSessionStatus.SUCCESS, sessionRepository.findByJti("other-active")?.status)
    }

    @Test
    fun `updates last used timestamp for non revoked successful sessions`() {
        val user = persistUser()
        sessionRepository.saveSuccessfulSession(
            successfulSession(
                userId = user.id,
                tokenJti = "last-used-active",
            ),
        )
        sessionRepository.saveSuccessfulSession(
            successfulSession(
                userId = user.id,
                tokenJti = "last-used-revoked",
                status = UserLoginSessionStatus.REVOKED,
                revokedAt = Instant.parse("2026-06-08T12:10:00Z"),
            ),
        )

        assertTrue(
            sessionRepository.updateLastUsedAt(
                tokenJti = "last-used-active",
                lastUsedAt = Instant.parse("2026-06-08T12:20:00Z"),
            ),
        )
        assertFalse(
            sessionRepository.updateLastUsedAt(
                tokenJti = "last-used-revoked",
                lastUsedAt = Instant.parse("2026-06-08T12:20:00Z"),
            ),
        )

        assertEquals(
            Instant.parse("2026-06-08T12:20:00Z"),
            sessionRepository.findByJti("last-used-active")?.lastUsedAt,
        )
        assertNull(sessionRepository.findByJti("last-used-revoked")?.lastUsedAt)
    }

    private fun persistUser(
        email: String = "owner@example.com",
        nickname: String = "owner-user",
    ): UserEntity {
        val user = UserEntity(
            id = UUID.randomUUID(),
            email = email,
            name = "Session Owner",
            nickname = nickname,
            passwordHash = "argon2id-hash",
            createdAt = Instant.parse("2026-06-08T12:00:00Z"),
            updatedAt = Instant.parse("2026-06-08T12:00:00Z"),
        )
        entityManager.persist(user)
        entityManager.flush()
        return user
    }

    private fun successfulSession(
        userId: UUID? = null,
        tokenJti: String,
        status: UserLoginSessionStatus = UserLoginSessionStatus.SUCCESS,
        revokedAt: Instant? = null,
        expiresAt: Instant? = Instant.parse("2026-06-08T13:00:00Z"),
    ): UserLoginSessionEntity =
        UserLoginSessionEntity(
            id = UUID.randomUUID(),
            userId = userId,
            attemptedEmail = "owner@example.com",
            status = status,
            tokenJti = tokenJti,
            tokenHash = "hashed-token",
            issuedAt = Instant.parse("2026-06-08T12:00:00Z"),
            expiresAt = expiresAt,
            revokedAt = revokedAt,
            lastUsedAt = null,
            ipAddress = "127.0.0.1",
            userAgent = "JUnit",
            createdAt = Instant.parse("2026-06-08T12:00:00Z"),
            updatedAt = Instant.parse("2026-06-08T12:00:00Z"),
        )

    private fun failedAttempt(
        attemptedEmail: String,
        reason: String,
        userId: UUID? = null,
    ): UserLoginSessionEntity =
        UserLoginSessionEntity(
            id = UUID.randomUUID(),
            userId = userId,
            attemptedEmail = attemptedEmail,
            status = UserLoginSessionStatus.FAILED,
            failureReason = reason,
            createdAt = Instant.parse("2026-06-08T12:00:00Z"),
            updatedAt = Instant.parse("2026-06-08T12:00:00Z"),
        )
}
