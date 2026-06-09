package com.lhn.favs_list.auth

import com.lhn.favs_list.shared.config.AuthJwtProperties
import java.time.Clock
import java.time.Instant
import java.util.UUID
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtException

class JwtTokenService(
    private val jwtEncoder: JwtEncoder,
    private val jwtDecoder: JwtDecoder,
    private val authJwtProperties: AuthJwtProperties,
    private val clock: Clock,
) : TokenService {

    override fun createAccessToken(userId: UUID, tokenJti: String): IssuedAccessToken {
        val issuedAt = clock.instant()
        val expiresAt = issuedAt.plusSeconds(authJwtProperties.accessTokenTtlSeconds)
        val claims = JwtClaimsSet.builder()
            .subject(userId.toString())
            .id(tokenJti)
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .issuer(authJwtProperties.issuer)
            .audience(listOf(authJwtProperties.audience))
            .build()
        val headers = JwsHeader.with(authJwtProperties.jwsAlgorithm()).build()
        val token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue

        return IssuedAccessToken(
            token = token,
            jti = tokenJti,
            issuedAt = issuedAt,
            expiresAt = expiresAt,
            expiresInSeconds = authJwtProperties.accessTokenTtlSeconds,
        )
    }

    override fun validateAccessToken(token: String): AccessTokenClaims =
        try {
            val jwt = jwtDecoder.decode(token)
            AccessTokenClaims(
                userId = parseUserId(jwt.subject),
                jti = jwt.id?.takeIf(String::isNotBlank)
                    ?: throw BadJwtException("Missing token id"),
                issuedAt = jwt.issuedAt
                    ?: throw BadJwtException("Missing issued-at timestamp"),
                expiresAt = jwt.expiresAt
                    ?: throw BadJwtException("Missing expiration timestamp"),
                issuer = jwt.getClaimAsString("iss")
                    ?: throw BadJwtException("Missing issuer"),
                audience = jwt.audience,
            )
        } catch (exception: JwtException) {
            throw InvalidAccessTokenException(
                message = "Access token is invalid",
                cause = exception,
            )
        }

    private fun parseUserId(subject: String?): UUID =
        try {
            UUID.fromString(subject ?: throw BadJwtException("Missing subject"))
        } catch (exception: IllegalArgumentException) {
            throw BadJwtException("Subject must be a UUID", exception)
        }
}

class InvalidAccessTokenException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
