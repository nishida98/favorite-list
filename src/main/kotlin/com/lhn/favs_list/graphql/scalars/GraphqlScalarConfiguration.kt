package com.lhn.favs_list.graphql.scalars

import graphql.ErrorType
import graphql.language.StringValue
import graphql.schema.Coercing
import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import graphql.schema.CoercingSerializeException
import graphql.schema.GraphQLScalarType
import graphql.schema.idl.RuntimeWiring
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.UUID
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.execution.RuntimeWiringConfigurer

@Configuration
class GraphqlScalarConfiguration {

    @Bean
    fun runtimeWiringConfigurer(): RuntimeWiringConfigurer =
        RuntimeWiringConfigurer { builder: RuntimeWiring.Builder ->
            builder.scalar(dateTimeScalar())
            builder.scalar(uuidScalar())
        }

    @Bean
    fun dateTimeScalar(): GraphQLScalarType =
        GraphQLScalarType.newScalar()
            .name("DateTime")
            .description("ISO-8601 UTC timestamp")
            .coercing(
                object : Coercing<Instant, String> {
                    override fun serialize(dataFetcherResult: Any): String =
                        when (dataFetcherResult) {
                            is Instant -> dataFetcherResult.toString()
                            is String -> parseInstant(dataFetcherResult).toString()
                            else -> throw serializeError("DateTime must serialize from an Instant")
                        }

                    override fun parseValue(input: Any): Instant =
                        when (input) {
                            is String -> parseInstant(input)
                            else -> throw parseValueError("DateTime must be an ISO-8601 UTC string")
                        }

                    override fun parseLiteral(input: Any): Instant =
                        when (input) {
                            is StringValue -> input.value?.let {
                                try {
                                    Instant.parse(it)
                                } catch (exception: DateTimeParseException) {
                                    throw parseLiteralError("DateTime must be an ISO-8601 UTC string", exception)
                                }
                            } ?: throw parseLiteralError("DateTime must be an ISO-8601 UTC string")
                            else -> throw parseLiteralError("DateTime must be an ISO-8601 UTC string")
                        }
                },
            )
            .build()

    @Bean
    fun uuidScalar(): GraphQLScalarType =
        GraphQLScalarType.newScalar()
            .name("UUID")
            .description("UUID string")
            .coercing(
                object : Coercing<UUID, String> {
                    override fun serialize(dataFetcherResult: Any): String =
                        when (dataFetcherResult) {
                            is UUID -> dataFetcherResult.toString()
                            is String -> parseUuid(dataFetcherResult).toString()
                            else -> throw serializeError("UUID must serialize from a UUID value")
                        }

                    override fun parseValue(input: Any): UUID =
                        when (input) {
                            is String -> parseUuid(input)
                            else -> throw parseValueError("UUID must be a valid UUID string")
                        }

                    override fun parseLiteral(input: Any): UUID =
                        when (input) {
                            is StringValue -> input.value?.let {
                                try {
                                    UUID.fromString(it)
                                } catch (exception: IllegalArgumentException) {
                                    throw parseLiteralError("UUID must be a valid UUID string", exception)
                                }
                            } ?: throw parseLiteralError("UUID must be a valid UUID string")
                            else -> throw parseLiteralError("UUID must be a valid UUID string")
                        }
                },
            )
            .build()

    private fun parseInstant(value: String): Instant =
        try {
            Instant.parse(value)
        } catch (exception: DateTimeParseException) {
            throw parseValueError("DateTime must be an ISO-8601 UTC string", exception)
        }

    private fun parseUuid(value: String): UUID =
        try {
            UUID.fromString(value)
        } catch (exception: IllegalArgumentException) {
            throw parseValueError("UUID must be a valid UUID string", exception)
        }

    private fun parseValueError(
        message: String,
        cause: Throwable? = null,
    ): CoercingParseValueException =
        CoercingParseValueException.newCoercingParseValueException()
            .message(message)
            .cause(cause)
            .errorClassification(ErrorType.ValidationError)
            .extensions(mapOf("code" to "BAD_USER_INPUT"))
            .build()

    private fun parseLiteralError(
        message: String,
        cause: Throwable? = null,
    ): CoercingParseLiteralException =
        CoercingParseLiteralException.newCoercingParseLiteralException()
            .message(message)
            .cause(cause)
            .errorClassification(ErrorType.ValidationError)
            .extensions(mapOf("code" to "BAD_USER_INPUT"))
            .build()

    private fun serializeError(message: String): CoercingSerializeException =
        CoercingSerializeException.newCoercingSerializeException()
            .message(message)
            .errorClassification(ErrorType.DataFetchingException)
            .extensions(mapOf("code" to "INTERNAL_SERVER_ERROR"))
            .build()
}
