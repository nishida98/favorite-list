package com.lhn.favs_list.shared.ids

import java.security.SecureRandom
import java.time.Clock
import java.util.UUID

// TODO: search for lib that generates UUIDv7
fun interface UuidGenerator {
    fun randomUuid(): UUID

    companion object {
        fun uuidV7(clock: Clock): UuidGenerator =
            UuidV7Generator(clock = clock)
    }
}

private class UuidV7Generator(
    private val clock: Clock,
    private val secureRandom: SecureRandom = SecureRandom(),
) : UuidGenerator {

    override fun randomUuid(): UUID {
        val timestampMillis = clock.millis()
        val randomBytes = ByteArray(10)
        secureRandom.nextBytes(randomBytes)

        val mostSignificantBits =
            ((timestampMillis and 0xFFFF_FFFF_FFFFL) shl 16) or
                (0x7L shl 12) or
                ((randomBytes[0].toLong() and 0xFFL) shl 4) or
                ((randomBytes[1].toLong() and 0xF0L) ushr 4)

        val leastSignificantBits =
            (0x2L shl 62) or
                ((randomBytes[1].toLong() and 0x0FL) shl 58) or
                ((randomBytes[2].toLong() and 0xFFL) shl 50) or
                ((randomBytes[3].toLong() and 0xFFL) shl 42) or
                ((randomBytes[4].toLong() and 0xFFL) shl 34) or
                ((randomBytes[5].toLong() and 0xFFL) shl 26) or
                ((randomBytes[6].toLong() and 0xFFL) shl 18) or
                ((randomBytes[7].toLong() and 0xFFL) shl 10) or
                ((randomBytes[8].toLong() and 0xFFL) shl 2) or
                ((randomBytes[9].toLong() and 0xC0L) ushr 6)

        return UUID(mostSignificantBits, leastSignificantBits)
    }
}
