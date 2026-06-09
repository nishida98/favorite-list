package com.lhn.favs_list.users

import com.lhn.favs_list.users.persistence.UserEntity
import java.time.Instant
import java.util.UUID

data class UserProfile(
    val id: UUID,
    val email: String,
    val name: String,
    val nickname: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

internal fun UserEntity.toUserProfile(): UserProfile =
    UserProfile(
        id = id,
        email = email,
        name = name,
        nickname = nickname,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
