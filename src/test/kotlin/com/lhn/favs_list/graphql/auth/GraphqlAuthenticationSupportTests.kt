package com.lhn.favs_list.graphql.auth

import com.lhn.favs_list.auth.AccessTokenClaims
import com.lhn.favs_list.auth.InvalidAccessTokenException
import com.lhn.favs_list.auth.IssuedAccessToken
import com.lhn.favs_list.auth.TokenService
import com.lhn.favs_list.graphql.GraphqlAuthentication
import com.lhn.favs_list.graphql.GraphqlRequestContext
import com.lhn.favs_list.graphql.UnauthenticatedGraphqlException
import com.lhn.favs_list.shared.logging.SecurityEventLogger
import com.lhn.favs_list.sessions.persistence.UserLoginSessionEntity
import com.lhn.favs_list.sessions.persistence.UserLoginSessionStatus
import com.lhn.favs_list.testing.InMemoryUserLoginSessionRepository
import com.lhn.favs_list.testing.InMemoryUserRepository
import com.lhn.favs_list.testing.RecordingSecurityEventSink
import com.lhn.favs_list.users.persistence.UserEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import org.springframework.http.HttpHeaders

class GraphqlAuthenticationSupportTests {

    private val clock = Clock.fixed(Instant.parse("2026-06-08T12:00:00Z"), ZoneOffset.UTC)
    private val userRepository = InMemoryUserRepository()
    private val sessionRepository = InMemoryUserLoginSessionRepository()
    private val tokenService = StubValidatingTokenService()
    private val securityEventSink = RecordingSecurityEventSink()
    private val factory = GraphqlAuthenticationContextFactory(
        tokenService = tokenService,
        sessionRepository = sessionRepository,
        userRepository = userRepository,
        clock = clock,
        securityEventLogger = SecurityEventLogger(securityEventSink),
    )
    private val authGuard = GraphqlAuthGuard()

    @Test
    fun `creates missing authentication context when authorization header is absent`() {
        val headers = HttpHeaders().apply {
            add(HttpHeaders.USER_AGENT, "JUnit")
        }

        val requestContext = factory.create(
            requestId = "request-1",
            headers = headers,
            remoteAddress = "127.0.0.1",
        )

        assertEquals("request-1", requestContext.requestId)
        assertEquals("127.0.0.1", requestContext.ipAddress)
        assertEquals("JUnit", requestContext.userAgent)
        assertEquals(GraphqlAuthentication.Missing, requestContext.authentication)
    }

    @Test
    fun `flags malformed bearer headers without authenticating the request`() {
        val headers = HttpHeaders().apply {
            add(HttpHeaders.AUTHORIZATION, "Basic abc123")
        }

        val requestContext = factory.create(
            requestId = "request-2",
            headers = headers,
            remoteAddress = null,
        )

        val failure = assertIs<GraphqlAuthentication.Failed>(requestContext.authentication)
        assertEquals("Authorization header must use Bearer authentication", failure.cause.message)
        assertEquals("auth_token_validation_failure", securityEventSink.entries.single().event)
    }

    @Test
    fun `authenticates requests only when token session and active user are valid`() {
        val user = activeUser(
            id = UUID.fromString("7e4d0303-8c91-4e68-8b67-e4677229c8f3"),
            email = "graphql@example.com",
        )
        userRepository.seed(user)
        val sessionId = UUID.fromString("0c3b70dd-2626-467f-9a25-bb93dd03a9b7")
        val tokenJti = "jti-123"
        sessionRepository.saveSuccessfulSession(
            UserLoginSessionEntity(
                id = sessionId,
                userId = user.id,
                attemptedEmail = user.email,
                status = UserLoginSessionStatus.SUCCESS,
                tokenJti = tokenJti,
                tokenHash = "hash",
                issuedAt = Instant.parse("2026-06-08T11:00:00Z"),
                expiresAt = Instant.parse("2026-06-08T13:00:00Z"),
                createdAt = Instant.parse("2026-06-08T11:00:00Z"),
                updatedAt = Instant.parse("2026-06-08T11:00:00Z"),
            ),
        )
        tokenService.claimsByToken["valid-token"] = AccessTokenClaims(
            userId = user.id,
            jti = tokenJti,
            issuedAt = Instant.parse("2026-06-08T11:00:00Z"),
            expiresAt = Instant.parse("2026-06-08T13:00:00Z"),
            issuer = "issuer",
            audience = listOf("audience"),
        )

        val requestContext = factory.create(
            requestId = "request-3",
            headers = HttpHeaders().apply {
                add(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
            },
            remoteAddress = "10.0.0.1",
        )

        val authenticated = assertIs<GraphqlAuthentication.Authenticated>(requestContext.authentication)
        assertEquals(user.id, authenticated.userId)
        assertEquals(sessionId, authenticated.sessionId)
        assertEquals(tokenJti, authenticated.sessionJti)
    }

    @Test
    fun `rejects protected operations when session is revoked or user is deleted`() {
        val user = activeUser(
            id = UUID.fromString("623a42f2-d3da-49bf-bf4c-e9aab7cb5a70"),
            email = "deleted@example.com",
        )
        user.deletedAt = Instant.parse("2026-06-08T11:30:00Z")
        userRepository.seed(user)
        sessionRepository.saveSuccessfulSession(
            UserLoginSessionEntity(
                id = UUID.fromString("bf4a4ed7-70bb-4b8a-b773-c81f54d5170c"),
                userId = user.id,
                attemptedEmail = user.email,
                status = UserLoginSessionStatus.REVOKED,
                tokenJti = "revoked-jti",
                tokenHash = "hash",
                issuedAt = Instant.parse("2026-06-08T11:00:00Z"),
                expiresAt = Instant.parse("2026-06-08T13:00:00Z"),
                revokedAt = Instant.parse("2026-06-08T11:45:00Z"),
                createdAt = Instant.parse("2026-06-08T11:00:00Z"),
                updatedAt = Instant.parse("2026-06-08T11:45:00Z"),
            ),
        )
        tokenService.claimsByToken["revoked-token"] = AccessTokenClaims(
            userId = user.id,
            jti = "revoked-jti",
            issuedAt = Instant.parse("2026-06-08T11:00:00Z"),
            expiresAt = Instant.parse("2026-06-08T13:00:00Z"),
            issuer = "issuer",
            audience = listOf("audience"),
        )

        val requestContext = factory.create(
            requestId = "request-4",
            headers = HttpHeaders().apply {
                add(HttpHeaders.AUTHORIZATION, "Bearer revoked-token")
            },
            remoteAddress = null,
        )

        val failure = assertIs<GraphqlAuthentication.Failed>(requestContext.authentication)
        assertEquals("Access token is invalid", failure.cause.message)
        assertEquals("auth_token_validation_failure", securityEventSink.entries.single().event)
    }

    @Test
    fun `guard rejects missing or failed authentication and returns the authenticated session otherwise`() {
        assertFailsWith<UnauthenticatedGraphqlException> {
            authGuard.requireAuthenticated(requestContext(authentication = GraphqlAuthentication.Missing))
        }
        assertFailsWith<UnauthenticatedGraphqlException> {
            authGuard.requireAuthenticated(
                requestContext(
                    authentication = GraphqlAuthentication.Failed(
                        UnauthenticatedGraphqlException("Access token is invalid"),
                    ),
                ),
            )
        }

        val authenticated = authGuard.requireAuthenticated(
            requestContext(
                authentication = GraphqlAuthentication.Authenticated(
                    userId = UUID.fromString("74db28e3-fe74-4f42-a934-fcde639953a1"),
                    sessionId = UUID.fromString("df0580e8-c4a6-444a-8eeb-ef8f9f3f28a6"),
                    sessionJti = "active-jti",
                ),
            ),
        )

        assertEquals("active-jti", authenticated.sessionJti)
    }

    private fun requestContext(authentication: GraphqlAuthentication) =
        GraphqlRequestContext(
            requestId = "request",
            ipAddress = null,
            userAgent = null,
            authentication = authentication,
        )

    private fun activeUser(
        id: UUID,
        email: String,
    ) = UserEntity(
        id = id,
        email = email,
        name = "GraphQL User",
        nickname = "graphql-user",
        passwordHash = "hash::Secret123",
        createdAt = Instant.parse("2026-06-08T10:00:00Z"),
        updatedAt = Instant.parse("2026-06-08T10:00:00Z"),
    )
}

private class StubValidatingTokenService : TokenService {
    val claimsByToken = linkedMapOf<String, AccessTokenClaims>()

    override fun createAccessToken(userId: UUID, tokenJti: String): IssuedAccessToken =
        throw UnsupportedOperationException("Not needed in these tests")

    override fun validateAccessToken(token: String): AccessTokenClaims =
        claimsByToken[token] ?: throw InvalidAccessTokenException("Access token is invalid")
}
