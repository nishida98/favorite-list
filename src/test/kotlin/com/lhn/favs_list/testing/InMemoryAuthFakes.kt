package com.lhn.favs_list.testing

import com.lhn.favs_list.auth.IssuedAccessToken
import com.lhn.favs_list.auth.PasswordHasher
import com.lhn.favs_list.auth.TokenService
import com.lhn.favs_list.shared.logging.SecurityEventLogEntry
import com.lhn.favs_list.shared.logging.SecurityEventSink
import com.lhn.favs_list.sessions.UserLoginSessionRepository
import com.lhn.favs_list.sessions.persistence.UserLoginSessionEntity
import com.lhn.favs_list.sessions.persistence.UserLoginSessionStatus
import com.lhn.favs_list.shared.ids.UuidGenerator
import com.lhn.favs_list.users.UserRepository
import com.lhn.favs_list.users.persistence.UserEntity
import java.time.Instant
import java.util.ArrayDeque
import java.util.UUID

class InMemoryUserRepository : UserRepository {
    private val usersById = linkedMapOf<UUID, UserEntity>()

    override fun findActiveById(id: UUID): UserEntity? =
        usersById[id]?.takeIf { it.deletedAt == null }

    override fun findByNormalizedEmail(normalizedEmail: String): UserEntity? =
        usersById.values.firstOrNull { it.email == normalizedEmail }

    override fun findActiveByNormalizedEmail(normalizedEmail: String): UserEntity? =
        usersById.values.firstOrNull { it.email == normalizedEmail && it.deletedAt == null }

    override fun existsByNormalizedEmail(normalizedEmail: String): Boolean =
        usersById.values.any { it.email == normalizedEmail }

    override fun save(user: UserEntity): UserEntity {
        usersById[user.id] = user
        return user
    }

    override fun update(user: UserEntity): UserEntity {
        usersById[user.id] = user
        return user
    }

    fun seed(user: UserEntity) {
        usersById[user.id] = user
    }
}

class InMemoryUserLoginSessionRepository : UserLoginSessionRepository {
    val successfulSessions = linkedMapOf<String, UserLoginSessionEntity>()
    val failedAttempts = mutableListOf<UserLoginSessionEntity>()

    override fun saveSuccessfulSession(session: UserLoginSessionEntity): UserLoginSessionEntity {
        successfulSessions[session.tokenJti!!] = session
        return session
    }

    override fun saveFailedLoginAttempt(session: UserLoginSessionEntity): UserLoginSessionEntity {
        failedAttempts += session
        return session
    }

    override fun findByJti(tokenJti: String): UserLoginSessionEntity? =
        successfulSessions[tokenJti]

    override fun revokeByJti(tokenJti: String, revokedAt: Instant, activeAt: Instant): Boolean {
        val session = successfulSessions[tokenJti] ?: return false
        if (session.status != UserLoginSessionStatus.SUCCESS || session.revokedAt != null) {
            return false
        }
        if (session.expiresAt != null && session.expiresAt!! <= activeAt) {
            return false
        }

        session.status = UserLoginSessionStatus.REVOKED
        session.revokedAt = revokedAt
        session.updatedAt = revokedAt
        return true
    }

    override fun revokeActiveSessionsByUserId(userId: UUID, revokedAt: Instant, activeAt: Instant): Int {
        var revokedCount = 0
        successfulSessions.values
            .filter { it.userId == userId }
            .forEach { session ->
                if (session.status == UserLoginSessionStatus.SUCCESS &&
                    session.revokedAt == null &&
                    (session.expiresAt == null || session.expiresAt!! > activeAt)
                ) {
                    session.status = UserLoginSessionStatus.REVOKED
                    session.revokedAt = revokedAt
                    session.updatedAt = revokedAt
                    revokedCount += 1
                }
            }

        return revokedCount
    }

    override fun updateLastUsedAt(tokenJti: String, lastUsedAt: Instant): Boolean {
        val session = successfulSessions[tokenJti] ?: return false
        if (session.status != UserLoginSessionStatus.SUCCESS || session.revokedAt != null) {
            return false
        }

        session.lastUsedAt = lastUsedAt
        session.updatedAt = lastUsedAt
        return true
    }
}

class StubPasswordHasher : PasswordHasher {
    override fun hash(rawPassword: String): String = "hash::$rawPassword"

    override fun matches(rawPassword: String, passwordHash: String): Boolean =
        hash(rawPassword) == passwordHash
}

class StubTokenService(
    private val issuedAt: Instant,
    private val expiresAt: Instant,
) : TokenService {
    override fun createAccessToken(userId: UUID, tokenJti: String): IssuedAccessToken =
        IssuedAccessToken(
            token = "token-for-$tokenJti",
            jti = tokenJti,
            issuedAt = issuedAt,
            expiresAt = expiresAt,
            expiresInSeconds = expiresAt.epochSecond - issuedAt.epochSecond,
        )

    override fun validateAccessToken(token: String) =
        throw UnsupportedOperationException("Not needed in these tests")
}

class QueueUuidGenerator(vararg uuids: UUID) : UuidGenerator {
    private val queue = ArrayDeque(uuids.asList())

    override fun randomUuid(): UUID =
        if (queue.isEmpty()) {
            error("No UUIDs left in the test generator")
        } else {
            queue.removeFirst()
        }
}

class RecordingSecurityEventSink : SecurityEventSink {
    val entries = mutableListOf<SecurityEventLogEntry>()

    override fun publish(entry: SecurityEventLogEntry) {
        entries += entry
    }
}
