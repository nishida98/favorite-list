package com.lhn.favs_list.shared.logging

import com.lhn.favs_list.auth.LoginFailureReason
import com.lhn.favs_list.auth.RequestMetadata
import com.lhn.favs_list.testing.RecordingSecurityEventSink
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityEventLoggerTests {

    private val sink = RecordingSecurityEventSink()
    private val logger = SecurityEventLogger(sink)

    @Test
    fun `hashes email and token identifiers before publishing events`() {
        logger.loginFailed(
            attemptedEmail = "user@example.com",
            userId = UUID.fromString("1738e4af-0f45-43d6-9d32-95b929b08ef9"),
            reason = LoginFailureReason.INVALID_PASSWORD,
            requestMetadata = RequestMetadata(requestId = "request-1", ipAddress = "127.0.0.1"),
        )
        logger.logoutSucceeded(
            userId = UUID.fromString("1738e4af-0f45-43d6-9d32-95b929b08ef9"),
            sessionJti = "raw-jti-value",
        )

        val loginFailure = sink.entries[0]
        val logoutSuccess = sink.entries[1]

        assertEquals("auth_login_failure", loginFailure.event)
        assertFalse(loginFailure.fields.containsValue("user@example.com"))
        assertTrue(loginFailure.fields["emailHash"].toString().isNotBlank())

        assertEquals("auth_logout_success", logoutSuccess.event)
        assertFalse(logoutSuccess.fields.containsValue("raw-jti-value"))
        assertTrue(logoutSuccess.fields["tokenJtiHash"].toString().isNotBlank())
    }
}
