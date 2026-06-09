package com.lhn.favs_list.sessions

import com.lhn.favs_list.sessions.persistence.UserLoginSessionEntity
import java.time.Instant
import java.util.UUID

interface UserLoginSessionRepository {
    fun saveSuccessfulSession(session: UserLoginSessionEntity): UserLoginSessionEntity

    fun saveFailedLoginAttempt(session: UserLoginSessionEntity): UserLoginSessionEntity

    fun findByJti(tokenJti: String): UserLoginSessionEntity?

    fun revokeByJti(tokenJti: String, revokedAt: Instant, activeAt: Instant = revokedAt): Boolean

    fun revokeActiveSessionsByUserId(userId: UUID, revokedAt: Instant, activeAt: Instant = revokedAt): Int

    fun updateLastUsedAt(tokenJti: String, lastUsedAt: Instant): Boolean
}
