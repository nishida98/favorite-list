package com.lhn.favs_list.graphql

import java.util.UUID

const val GRAPHQL_REQUEST_CONTEXT_KEY = "requestContext"

data class GraphqlRequestContext(
    val requestId: String,
    val ipAddress: String?,
    val userAgent: String?,
    val authentication: GraphqlAuthentication = GraphqlAuthentication.Missing,
)

sealed interface GraphqlAuthentication {
    data object Missing : GraphqlAuthentication

    data class Failed(
        val cause: UnauthenticatedGraphqlException,
    ) : GraphqlAuthentication

    data class Authenticated(
        val userId: UUID,
        val sessionId: UUID,
        val sessionJti: String,
    ) : GraphqlAuthentication
}

open class UnauthenticatedGraphqlException(
    message: String = "Authentication is required",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class ForbiddenGraphqlException(
    message: String = "Forbidden",
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class RateLimitedGraphqlException(
    message: String = "Too many requests",
    cause: Throwable? = null,
) : RuntimeException(message, cause)
