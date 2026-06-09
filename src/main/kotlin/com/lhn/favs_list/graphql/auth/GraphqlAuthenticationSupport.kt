package com.lhn.favs_list.graphql.auth

import com.lhn.favs_list.auth.InvalidAccessTokenException
import com.lhn.favs_list.auth.RequestMetadata
import com.lhn.favs_list.auth.TokenService
import com.lhn.favs_list.graphql.GRAPHQL_REQUEST_CONTEXT_KEY
import com.lhn.favs_list.graphql.GraphqlAuthentication
import com.lhn.favs_list.graphql.GraphqlRequestContext
import com.lhn.favs_list.graphql.UnauthenticatedGraphqlException
import com.lhn.favs_list.sessions.UserLoginSessionRepository
import com.lhn.favs_list.sessions.persistence.UserLoginSessionStatus
import com.lhn.favs_list.users.UserRepository
import graphql.GraphQLContext
import java.time.Clock
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component

@Component
class GraphqlAuthenticationContextFactory(
    private val tokenService: TokenService,
    private val sessionRepository: UserLoginSessionRepository,
    private val userRepository: UserRepository,
    private val clock: Clock,
) {

    fun create(
        requestId: String,
        headers: HttpHeaders,
        remoteAddress: String?,
    ): GraphqlRequestContext {
        val authentication = resolveAuthentication(headers)

        return GraphqlRequestContext(
            requestId = requestId,
            ipAddress = remoteAddress,
            userAgent = headers.getFirst(HttpHeaders.USER_AGENT),
            authentication = authentication,
        )
    }

    private fun resolveAuthentication(headers: HttpHeaders): GraphqlAuthentication {
        val token = try {
            AuthorizationHeaderBearerTokenExtractor.extract(headers.getFirst(HttpHeaders.AUTHORIZATION))
        } catch (exception: UnauthenticatedGraphqlException) {
            return GraphqlAuthentication.Failed(exception)
        }

        if (token == null) {
            return GraphqlAuthentication.Missing
        }

        val claims = try {
            tokenService.validateAccessToken(token)
        } catch (exception: InvalidAccessTokenException) {
            return GraphqlAuthentication.Failed(
                UnauthenticatedGraphqlException(
                    message = "Access token is invalid",
                    cause = exception,
                ),
            )
        }

        val session = sessionRepository.findByJti(claims.jti)
            ?: return GraphqlAuthentication.Failed(
                UnauthenticatedGraphqlException("Access token is invalid"),
            )
        val now = clock.instant()
        if (session.status != UserLoginSessionStatus.SUCCESS ||
            session.revokedAt != null ||
            session.expiresAt?.let { !it.isAfter(now) } == true ||
            session.userId != claims.userId
        ) {
            return GraphqlAuthentication.Failed(
                UnauthenticatedGraphqlException("Access token is invalid"),
            )
        }
        if (userRepository.findActiveById(claims.userId) == null) {
            return GraphqlAuthentication.Failed(
                UnauthenticatedGraphqlException("Access token is invalid"),
            )
        }

        return GraphqlAuthentication.Authenticated(
            userId = claims.userId,
            sessionId = session.id,
            sessionJti = claims.jti,
        )
    }
}

@Component
class GraphqlAuthGuard {

    fun requireAuthenticated(requestContext: GraphqlRequestContext?): GraphqlAuthentication.Authenticated =
        when (val authentication = requestContext?.authentication ?: GraphqlAuthentication.Missing) {
            GraphqlAuthentication.Missing -> throw UnauthenticatedGraphqlException("Authentication is required")
            is GraphqlAuthentication.Failed -> throw authentication.cause
            is GraphqlAuthentication.Authenticated -> authentication
        }
}

object AuthorizationHeaderBearerTokenExtractor {
    private const val BEARER_PREFIX = "Bearer "

    fun extract(headerValue: String?): String? {
        if (headerValue == null) {
            return null
        }
        if (!headerValue.startsWith(BEARER_PREFIX)) {
            throw UnauthenticatedGraphqlException("Authorization header must use Bearer authentication")
        }

        val token = headerValue.removePrefix(BEARER_PREFIX).trim()
        if (token.isEmpty()) {
            throw UnauthenticatedGraphqlException("Authorization header must use Bearer authentication")
        }

        return token
    }
}

fun GraphQLContext.requestContext(): GraphqlRequestContext? =
    get(GRAPHQL_REQUEST_CONTEXT_KEY)

fun GraphqlRequestContext.toRequestMetadata(): RequestMetadata =
    RequestMetadata(
        ipAddress = ipAddress,
        userAgent = userAgent,
    )
