package com.lhn.favs_list.users.persistence

import com.lhn.favs_list.users.UserRepository
import java.util.UUID
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class JpaUserRepository(
    private val springDataRepository: SpringDataUserEntityRepository,
) : UserRepository {

    @Transactional(readOnly = true)
    override fun findActiveById(id: UUID): UserEntity? =
        springDataRepository.findActiveById(id)

    @Transactional(readOnly = true)
    override fun findActiveByNormalizedEmail(normalizedEmail: String): UserEntity? =
        springDataRepository.findActiveByEmail(normalizedEmail)

    @Transactional(readOnly = true)
    override fun existsByNormalizedEmail(normalizedEmail: String): Boolean =
        springDataRepository.existsByEmail(normalizedEmail)

    @Transactional(readOnly = true)
    override fun existsByNicknameIgnoreCase(nickname: String, excludeUserId: UUID?): Boolean =
        springDataRepository.existsByNicknameIgnoreCase(nickname, excludeUserId)

    @Transactional
    override fun save(user: UserEntity): UserEntity =
        springDataRepository.saveAndFlush(user)

    @Transactional
    override fun update(user: UserEntity): UserEntity =
        springDataRepository.saveAndFlush(user)
}
