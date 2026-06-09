package com.lhn.favs_list.graphql

import com.lhn.favs_list.graphql.auth.GraphqlAuthenticationContextFactory
import graphql.ErrorType
import graphql.GraphqlErrorBuilder
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.graphql.ResponseError
import org.springframework.graphql.server.WebGraphQlInterceptor
import org.springframework.graphql.server.WebGraphQlRequest
import org.springframework.graphql.server.WebGraphQlResponse
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class GraphqlRequestContextInterceptor(
    private val authenticationContextFactory: GraphqlAuthenticationContextFactory,
) : WebGraphQlInterceptor {

    override fun intercept(
        request: WebGraphQlRequest,
        chain: WebGraphQlInterceptor.Chain,
    ): Mono<WebGraphQlResponse> {
        val requestContext = authenticationContextFactory.create(
            requestId = request.id,
            headers = request.headers,
            remoteAddress = request.remoteAddress?.address?.hostAddress,
        )
        request.configureExecutionInput { _, builder ->
            builder.graphQLContext { context ->
                context.put(GRAPHQL_REQUEST_CONTEXT_KEY, requestContext)
            }.build()
        }

        return chain.next(request)
    }
}

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class GraphqlErrorResponseInterceptor : WebGraphQlInterceptor {

    override fun intercept(
        request: WebGraphQlRequest,
        chain: WebGraphQlInterceptor.Chain,
    ): Mono<WebGraphQlResponse> =
        chain.next(request)
            .map { response ->
                val remappedErrors = response.errors.map { error ->
                    withApplicationErrorCode(error)
                }
                response.transform { builder ->
                    builder.errors(remappedErrors)
                }
            }

    private fun withApplicationErrorCode(error: ResponseError) =
        if (error.extensions.orEmpty().containsKey("code")) {
            GraphqlErrorBuilder.newError()
                .message(error.message)
                .locations(error.locations)
                .path(error.parsedPath)
                .errorType(error.errorType)
                .extensions(error.extensions)
                .build()
        } else {
            val extensions = error.extensions.orEmpty()
            val code = when (error.errorType) {
                ErrorType.InvalidSyntax,
                ErrorType.ValidationError,
                ErrorType.NullValueInNonNullableField,
                -> "BAD_USER_INPUT"

                else -> "INTERNAL_SERVER_ERROR"
            }

            GraphqlErrorBuilder.newError()
                .message(error.message)
                .locations(error.locations)
                .path(error.parsedPath)
                .errorType(error.errorType)
                .extensions(extensions + ("code" to code))
                .build()
        }
}
