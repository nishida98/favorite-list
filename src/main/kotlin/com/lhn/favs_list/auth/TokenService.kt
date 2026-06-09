package com.lhn.favs_list.auth

import java.time.Instant
import java.util.UUID

interface TokenService {
    fun createAccessToken(userId: UUID, tokenJti: String): IssuedAccessToken

    fun validateAccessToken(token: String): AccessTokenClaims
}

data class IssuedAccessToken(
    val token: String,
    val jti: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val expiresInSeconds: Long,
)

data class AccessTokenClaims(
    val userId: UUID,
    val jti: String,
    val issuedAt: Instant,
    val expiresAt: Instant,
    val issuer: String,
    val audience: List<String>,
)
