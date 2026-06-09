package com.lhn.favs_list.graphql.errors

import com.lhn.favs_list.auth.AuthenticationFailedException
import com.lhn.favs_list.graphql.UnauthenticatedGraphqlException
import com.lhn.favs_list.shared.validation.FieldValidationError
import com.lhn.favs_list.shared.validation.InvalidInputException
import com.lhn.favs_list.users.CurrentUserNotFoundException
import graphql.ErrorType
import graphql.schema.DataFetchingEnvironmentImpl
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class GraphqlExceptionResolverTests {

    private val resolver = GraphqlExceptionResolver()
    private val environment = DataFetchingEnvironmentImpl.newDataFetchingEnvironment().build()

    @Test
    fun `maps invalid input errors to bad user input with field errors`() {
        val error = resolveSingleError(
            InvalidInputException(
                fieldErrors = listOf(FieldValidationError("email", "Email is required")),
            ),
        )

        assertEquals("Invalid input", error.message)
        assertEquals(ErrorType.ValidationError, error.errorType)
        assertEquals("BAD_USER_INPUT", error.extensions?.get("code"))
        val fieldErrors = error.extensions?.get("fieldErrors") as? List<*>
        assertNotNull(fieldErrors)
        assertEquals(mapOf("field" to "email", "message" to "Email is required"), fieldErrors.single())
    }

    @Test
    fun `maps auth and not found domain errors to the expected application codes`() {
        val authError = resolveSingleError(AuthenticationFailedException())
        val missingUserError = resolveSingleError(
            CurrentUserNotFoundException(UUID.fromString("ee0a35cd-29bd-444a-ac6a-97a31209d859")),
        )

        assertEquals("UNAUTHENTICATED", authError.extensions?.get("code"))
        assertEquals("Invalid email or password", authError.message)
        assertEquals("NOT_FOUND", missingUserError.extensions?.get("code"))
        assertEquals("User not found", missingUserError.message)
    }

    @Test
    fun `maps unexpected errors to an internal server error without leaking details`() {
        val error = resolveSingleError(IllegalStateException("database exploded"))

        assertEquals("Internal server error", error.message)
        assertEquals("INTERNAL_SERVER_ERROR", error.extensions?.get("code"))
        assertEquals(ErrorType.DataFetchingException, error.errorType)
    }

    @Test
    fun `maps request authentication failures to unauthenticated`() {
        val error = resolveSingleError(UnauthenticatedGraphqlException("Access token is invalid"))

        assertEquals("Access token is invalid", error.message)
        assertEquals("UNAUTHENTICATED", error.extensions?.get("code"))
    }

    private fun resolveSingleError(exception: Throwable) =
        resolver.resolveException(exception, environment).block()!!.single()
}
