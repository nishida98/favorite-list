package com.lhn.favs_list.users.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "users")
class UserEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID,
    @Column(nullable = false, length = 255)
    var email: String,
    @Column(nullable = false, length = 255)
    var name: String,
    @Column(nullable = false, length = 80)
    var nickname: String,
    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null,
)
