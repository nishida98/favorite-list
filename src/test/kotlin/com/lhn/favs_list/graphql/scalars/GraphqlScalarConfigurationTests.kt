package com.lhn.favs_list.graphql.scalars

import graphql.schema.CoercingParseLiteralException
import graphql.schema.CoercingParseValueException
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GraphqlScalarConfigurationTests {

    private val configuration = GraphqlScalarConfiguration()

    @Test
    fun `date time scalar serializes instants as ISO 8601 UTC strings`() {
        val scalar = configuration.dateTimeScalar()

        val serialized = scalar.coercing.serialize(Instant.parse("2026-06-08T12:00:00Z"))

        assertEquals("2026-06-08T12:00:00Z", serialized)
    }

    @Test
    fun `date time scalar rejects invalid values with bad user input code`() {
        val scalar = configuration.dateTimeScalar()

        val exception = assertFailsWith<CoercingParseValueException> {
            scalar.coercing.parseValue("not-a-timestamp")
        }

        assertEquals("DateTime must be an ISO-8601 UTC string", exception.message)
        assertEquals("BAD_USER_INPUT", exception.extensions["code"])
    }

    @Test
    fun `uuid scalar parses and serializes UUID values safely`() {
        val scalar = configuration.uuidScalar()
        val uuid = UUID.fromString("dc3c67ad-6a9a-4c0d-9ffc-efeb6cab5f8b")

        assertEquals(uuid, scalar.coercing.parseValue(uuid.toString()))
        assertEquals(uuid.toString(), scalar.coercing.serialize(uuid))
    }

    @Test
    fun `uuid scalar rejects invalid literals with bad user input code`() {
        val scalar = configuration.uuidScalar()

        val exception = assertFailsWith<CoercingParseValueException> {
            scalar.coercing.parseValue("invalid-uuid")
        }

        assertEquals("UUID must be a valid UUID string", exception.message)
        assertEquals("BAD_USER_INPUT", exception.extensions["code"])
    }
}
