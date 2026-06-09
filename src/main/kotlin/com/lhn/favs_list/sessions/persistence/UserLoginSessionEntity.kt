package com.lhn.favs_list.sessions.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "user_login_sessions")
class UserLoginSessionEntity(
    @Id
    @Column(nullable = false, updatable = false)
    var id: UUID,
    @Column(name = "user_id")
    var userId: UUID? = null,
    @Column(name = "attempted_email", nullable = false, length = 255)
    var attemptedEmail: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: UserLoginSessionStatus,
    @Column(name = "failure_reason", length = 80)
    var failureReason: String? = null,
    @Column(name = "token_jti", length = 255)
    var tokenJti: String? = null,
    @Column(name = "token_hash", length = 255)
    var tokenHash: String? = null,
    @Column(name = "issued_at")
    var issuedAt: Instant? = null,
    @Column(name = "expires_at")
    var expiresAt: Instant? = null,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null,
    @Column(name = "ip_address", length = 80)
    var ipAddress: String? = null,
    @Column(name = "user_agent", length = 512)
    var userAgent: String? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,
)
