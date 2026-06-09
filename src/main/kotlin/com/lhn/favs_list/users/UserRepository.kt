package com.lhn.favs_list.users

import com.lhn.favs_list.users.persistence.UserEntity
import java.util.UUID

interface UserRepository {
    fun findActiveById(id: UUID): UserEntity?

    fun findActiveByNormalizedEmail(normalizedEmail: String): UserEntity?

    fun existsByNormalizedEmail(normalizedEmail: String): Boolean

    fun existsByNicknameIgnoreCase(nickname: String, excludeUserId: UUID? = null): Boolean

    fun save(user: UserEntity): UserEntity

    fun update(user: UserEntity): UserEntity
}
