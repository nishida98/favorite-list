package com.lhn.favs_list.sessions.persistence

import com.lhn.favs_list.sessions.UserLoginSessionRepository
import java.time.Instant
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaUserLoginSessionRepository(
    private val springDataRepository: SpringDataUserLoginSessionEntityRepository,
) : UserLoginSessionRepository {

    @Transactional
    override fun saveSuccessfulSession(session: UserLoginSessionEntity): UserLoginSessionEntity =
        springDataRepository.saveAndFlush(session)

    @Transactional
    override fun saveFailedLoginAttempt(session: UserLoginSessionEntity): UserLoginSessionEntity =
        springDataRepository.saveAndFlush(session)

    @Transactional(readOnly = true)
    override fun findByJti(tokenJti: String): UserLoginSessionEntity? =
        springDataRepository.findByTokenJti(tokenJti)

    @Transactional
    override fun revokeByJti(tokenJti: String, revokedAt: Instant, activeAt: Instant): Boolean =
        springDataRepository.revokeActiveByTokenJti(tokenJti, revokedAt, activeAt) > 0

    @Transactional
    override fun revokeActiveSessionsByUserId(userId: UUID, revokedAt: Instant, activeAt: Instant): Int =
        springDataRepository.revokeActiveByUserId(userId, revokedAt, activeAt)

    @Transactional
    override fun updateLastUsedAt(tokenJti: String, lastUsedAt: Instant): Boolean =
        springDataRepository.updateLastUsedAt(tokenJti, lastUsedAt) > 0
}
