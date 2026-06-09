package com.lhn.favs_list.sessions.persistence

import java.time.Instant
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpringDataUserLoginSessionEntityRepository : JpaRepository<UserLoginSessionEntity, UUID> {
    fun findByTokenJti(tokenJti: String): UserLoginSessionEntity?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            UPDATE user_login_sessions
            SET status = 'REVOKED',
                revoked_at = :revokedAt,
                updated_at = :revokedAt
            WHERE token_jti = :tokenJti
              AND status = 'SUCCESS'
              AND revoked_at IS NULL
              AND (expires_at IS NULL OR expires_at > :activeAt)
        """,
        nativeQuery = true,
    )
    fun revokeActiveByTokenJti(
        @Param("tokenJti") tokenJti: String,
        @Param("revokedAt") revokedAt: Instant,
        @Param("activeAt") activeAt: Instant,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            UPDATE user_login_sessions
            SET status = 'REVOKED',
                revoked_at = :revokedAt,
                updated_at = :revokedAt
            WHERE user_id = :userId
              AND status = 'SUCCESS'
              AND revoked_at IS NULL
              AND (expires_at IS NULL OR expires_at > :activeAt)
        """,
        nativeQuery = true,
    )
    fun revokeActiveByUserId(
        @Param("userId") userId: UUID,
        @Param("revokedAt") revokedAt: Instant,
        @Param("activeAt") activeAt: Instant,
    ): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        value = """
            UPDATE user_login_sessions
            SET last_used_at = :lastUsedAt,
                updated_at = :lastUsedAt
            WHERE token_jti = :tokenJti
              AND status = 'SUCCESS'
              AND revoked_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateLastUsedAt(
        @Param("tokenJti") tokenJti: String,
        @Param("lastUsedAt") lastUsedAt: Instant,
    ): Int
}
