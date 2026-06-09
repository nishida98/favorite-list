package com.lhn.favs_list.graphql.errors

import com.lhn.favs_list.auth.AuthenticationFailedException
import com.lhn.favs_list.graphql.ForbiddenGraphqlException
import com.lhn.favs_list.graphql.RateLimitedGraphqlException
import com.lhn.favs_list.graphql.UnauthenticatedGraphqlException
import com.lhn.favs_list.shared.validation.InvalidInputException
import com.lhn.favs_list.users.CurrentUserNotFoundException
import com.lhn.favs_list.users.EmailAlreadyInUseException
import graphql.ErrorType
import graphql.GraphQLError
import graphql.GraphqlErrorBuilder
import graphql.schema.DataFetchingEnvironment
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter
import org.springframework.stereotype.Component

@Component
class GraphqlExceptionResolver : DataFetcherExceptionResolverAdapter() {

    override fun resolveToSingleError(
        exception: Throwable,
        environment: DataFetchingEnvironment,
    ): GraphQLError {
        val error = when (exception) {
            is InvalidInputException -> buildError(
                environment = environment,
                message = "Invalid input",
                code = "BAD_USER_INPUT",
                errorType = ErrorType.ValidationError,
                extraExtensions = mapOf(
                    "fieldErrors" to exception.fieldErrors.map { fieldError ->
                        mapOf(
                            "field" to fieldError.field,
                            "message" to fieldError.message,
                        )
                    },
                ),
            )

            is EmailAlreadyInUseException -> buildError(
                environment = environment,
                message = "Email is already in use",
                code = "CONFLICT",
                errorType = ErrorType.DataFetchingException,
            )

            is AuthenticationFailedException -> buildError(
                environment = environment,
                message = "Invalid email or password",
                code = "UNAUTHENTICATED",
                errorType = ErrorType.DataFetchingException,
            )

            is UnauthenticatedGraphqlException -> buildError(
                environment = environment,
                message = exception.message ?: "Authentication is required",
                code = "UNAUTHENTICATED",
                errorType = ErrorType.DataFetchingException,
            )

            is ForbiddenGraphqlException -> buildError(
                environment = environment,
                message = exception.message ?: "Forbidden",
                code = "FORBIDDEN",
                errorType = ErrorType.DataFetchingException,
            )

            is CurrentUserNotFoundException -> buildError(
                environment = environment,
                message = "User not found",
                code = "NOT_FOUND",
                errorType = ErrorType.DataFetchingException,
            )

            is RateLimitedGraphqlException -> buildError(
                environment = environment,
                message = exception.message ?: "Too many requests",
                code = "RATE_LIMITED",
                errorType = ErrorType.DataFetchingException,
            )

            else -> buildError(
                environment = environment,
                message = "Internal server error",
                code = "INTERNAL_SERVER_ERROR",
                errorType = ErrorType.DataFetchingException,
            )
        }

        if (exception is InvalidInputException ||
            exception is EmailAlreadyInUseException ||
            exception is AuthenticationFailedException ||
            exception is UnauthenticatedGraphqlException ||
            exception is ForbiddenGraphqlException ||
            exception is CurrentUserNotFoundException ||
            exception is RateLimitedGraphqlException
        ) {
            logger.debug("GraphQL request failed: ${exception.message}")
        } else {
            logger.error("Unexpected GraphQL error", exception)
        }

        return error
    }

    private fun buildError(
        environment: DataFetchingEnvironment,
        message: String,
        code: String,
        errorType: ErrorType,
        extraExtensions: Map<String, Any> = emptyMap(),
    ): GraphQLError {
        val builder = GraphqlErrorBuilder.newError()
            .message(message)
            .errorType(errorType)
            .extensions(extraExtensions + ("code" to code))

        runCatching { environment.executionStepInfo.path.toList() }
            .getOrNull()
            ?.let(builder::path)
        runCatching { environment.field.sourceLocation }
            .getOrNull()
            ?.let(builder::location)

        return builder.build()
    }
}
