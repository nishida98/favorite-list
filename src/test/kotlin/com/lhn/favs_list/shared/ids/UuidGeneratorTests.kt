package com.lhn.favs_list.shared.ids

import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UuidGeneratorTests {

    @Test
    fun `generates uuid version 7 values using the current timestamp`() {
        val clock = Clock.fixed(Instant.parse("2026-06-08T12:34:56.789Z"), ZoneOffset.UTC)
        val generator = UuidGenerator.uuidV7(clock)

        val uuid = generator.randomUuid()
        val extractedTimestampMillis = uuid.mostSignificantBits ushr 16

        assertEquals(7, uuid.version())
        assertEquals(2, uuid.variant())
        assertEquals(clock.instant().toEpochMilli(), extractedTimestampMillis)
    }

    @Test
    fun `generates distinct uuid v7 values within the same millisecond`() {
        val clock = Clock.fixed(Instant.parse("2026-06-08T12:34:56.789Z"), ZoneOffset.UTC)
        val generator = UuidGenerator.uuidV7(clock)

        val first = generator.randomUuid()
        val second = generator.randomUuid()

        assertTrue(first != second)
        assertEquals(7, first.version())
        assertEquals(7, second.version())
    }
}
