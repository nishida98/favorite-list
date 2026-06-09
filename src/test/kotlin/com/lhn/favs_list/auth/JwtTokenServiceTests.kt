package com.lhn.favs_list.auth

import com.lhn.favs_list.shared.config.AuthJwtProperties
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class JwtTokenServiceTests {

    private val tokenConfiguration = JwtTokenConfiguration()

    @Test
    fun `creates and validates tokens with the expected claims`() {
        val clock = fixedClock("2026-06-08T12:00:00Z")
        val tokenService = tokenService(
            authJwtProperties = jwtProperties(),
            clock = clock,
        )
        val userId = UUID.fromString("4b210f7d-8235-4ea0-a0a1-af76aa39df14")

        val token = tokenService.createAccessToken(
            userId = userId,
            tokenJti = "session-123",
        )
        val claims = tokenService.validateAccessToken(token.token)

        assertEquals("session-123", claims.jti)
        assertEquals(userId, claims.userId)
        assertEquals(Instant.parse("2026-06-08T12:00:00Z"), claims.issuedAt)
        assertEquals(Instant.parse("2026-06-08T12:15:00Z"), claims.expiresAt)
        assertEquals("favs-list-api", claims.issuer)
        assertTrue(claims.audience.contains("favs-list-client"))
    }

    @Test
    fun `rejects expired tokens`() {
        val issuingClock = fixedClock("2026-06-08T12:00:00Z")
        val tokenService = tokenService(
            authJwtProperties = jwtProperties(accessTokenTtlSeconds = 60),
            clock = issuingClock,
        )
        val token = tokenService.createAccessToken(UUID.randomUUID(), "expired-token")
        val validatingService = tokenService(
            authJwtProperties = jwtProperties(accessTokenTtlSeconds = 60),
            clock = fixedClock("2026-06-08T12:01:01Z"),
        )

        assertFailsWith<InvalidAccessTokenException> {
            validatingService.validateAccessToken(token.token)
        }
    }

    @Test
    fun `rejects tokens signed with a different secret`() {
        val token = tokenService(
            authJwtProperties = jwtProperties(secret = "different-secret-value-1234567890"),
            clock = fixedClock("2026-06-08T12:00:00Z"),
        ).createAccessToken(UUID.randomUUID(), "bad-signature")

        assertFailsWith<InvalidAccessTokenException> {
            tokenService(
                authJwtProperties = jwtProperties(),
                clock = fixedClock("2026-06-08T12:00:00Z"),
            ).validateAccessToken(token.token)
        }
    }

    @Test
    fun `rejects tokens with the wrong issuer or audience`() {
        val wrongIssuerToken = tokenService(
            authJwtProperties = jwtProperties(issuer = "other-api"),
            clock = fixedClock("2026-06-08T12:00:00Z"),
        ).createAccessToken(UUID.randomUUID(), "wrong-issuer")
        val wrongAudienceToken = tokenService(
            authJwtProperties = jwtProperties(audience = "other-client"),
            clock = fixedClock("2026-06-08T12:00:00Z"),
        ).createAccessToken(UUID.randomUUID(), "wrong-audience")
        val validatingService = tokenService(
            authJwtProperties = jwtProperties(),
            clock = fixedClock("2026-06-08T12:00:00Z"),
        )

        assertFailsWith<InvalidAccessTokenException> {
            validatingService.validateAccessToken(wrongIssuerToken.token)
        }
        assertFailsWith<InvalidAccessTokenException> {
            validatingService.validateAccessToken(wrongAudienceToken.token)
        }
    }

    private fun tokenService(
        authJwtProperties: AuthJwtProperties,
        clock: Clock,
    ): TokenService {
        val encoder = tokenConfiguration.jwtEncoder(authJwtProperties)
        val decoder = tokenConfiguration.jwtDecoder(authJwtProperties, clock)

        return JwtTokenService(
            jwtEncoder = encoder,
            jwtDecoder = decoder,
            authJwtProperties = authJwtProperties,
            clock = clock,
        )
    }

    private fun jwtProperties(
        issuer: String = "favs-list-api",
        audience: String = "favs-list-client",
        accessTokenTtlSeconds: Long = 900,
        secret: String = "change-me-for-local-dev-1234567890",
    ) = AuthJwtProperties(
        issuer = issuer,
        audience = audience,
        accessTokenTtlSeconds = accessTokenTtlSeconds,
        secret = secret,
    )

    private fun fixedClock(instant: String): Clock =
        Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
}
