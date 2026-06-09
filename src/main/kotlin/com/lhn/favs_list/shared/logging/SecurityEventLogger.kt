package com.lhn.favs_list.shared.logging

import com.lhn.favs_list.auth.LoginFailureReason
import com.lhn.favs_list.auth.RequestMetadata
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SecurityEventLogger(
    private val sink: SecurityEventSink,
) {

    fun registrationSucceeded(
        userId: UUID,
        email: String,
    ) {
        sink.publish(
            SecurityEventLogEntry(
                level = SecurityEventLevel.INFO,
                event = "auth_register_success",
                message = "User registration succeeded",
                fields = mapOf(
                    "userId" to userId,
                    "emailHash" to fingerprint(email),
                ),
            ),
        )
    }

    fun registrationFailed(
        email: String,
        reason: String,
    ) {
        sink.publish(
            SecurityEventLogEntry(
                level = SecurityEventLevel.WARN,
                event = "auth_register_failure",
                message = "User registration failed",
                fields = mapOf(
                    "emailHash" to fingerprint(email),
                    "reason" to reason,
                ),
            ),
        )
    }

    fun loginSucceeded(
        userId: UUID,
        sessionId: UUID,
        tokenJti: String,
        requestMetadata: RequestMetadata,
    ) {
        sink.publish(
            SecurityEventLogEntry(
                level = SecurityEventLevel.INFO,
                event = "auth_login_success",
                message = "User login succeeded",
                fields = requestFields(requestMetadata) + mapOf(
                    "userId" to userId,
                    "sessionId" to sessionId,
                    "tokenJtiHash" to fingerprint(tokenJti),
                ),
            ),
        )
    }

    fun loginFailed(
        attemptedEmail: String,
        userId: UUID?,
        reason: LoginFailureReason,
        requestMetadata: RequestMetadata,
    ) {
        sink.publish(
            SecurityEventLogEntry(
                level = SecurityEventLevel.WARN,
                event = "auth_login_failure",
                message = "User login failed",
                fields = requestFields(requestMetadata) + buildMap {
                    put("emailHash", fingerprint(attemptedEmail))
                    put("reason", reason.name)
                    userId?.let { put("userId", it) }
                },
            ),
        )
    }

    fun tokenValidationFailed(
        requestId: String,
        ipAddress: String?,
        reason: String,
    ) {
        sink.publish(
            SecurityEventLogEntry(
                level = SecurityEventLevel.WARN,
                event = "auth_token_validation_failure",
                message = "Access token validation failed",
                fields = buildMap {
                    put("requestId", requestId)
                    put("reason", reason)
                    ipAddress?.let { put("ipAddress", it) }
                },
            ),
        )
    }

    fun logoutSucceeded(
        userId: UUID,
        sessionJti: String,
    ) {
        sink.publish(
            SecurityEventLogEntry(
                level = SecurityEventLevel.INFO,
                event = "auth_logout_success",
                message = "User logout succeeded",
                fields = mapOf(
                    "userId" to userId,
                    "tokenJtiHash" to fingerprint(sessionJti),
                ),
            ),
        )
    }

    fun userSoftDeleted(userId: UUID) {
        sink.publish(
            SecurityEventLogEntry(
                level = SecurityEventLevel.INFO,
                event = "auth_user_soft_delete",
                message = "User soft delete succeeded",
                fields = mapOf("userId" to userId),
            ),
        )
    }

    fun sessionRevocationSucceeded(
        userId: UUID,
        revokedSessionCount: Int,
        trigger: String,
    ) {
        sink.publish(
            SecurityEventLogEntry(
                level = SecurityEventLevel.INFO,
                event = "auth_session_revocation_success",
                message = "Session revocation succeeded",
                fields = mapOf(
                    "userId" to userId,
                    "revokedSessionCount" to revokedSessionCount,
                    "trigger" to trigger,
                ),
            ),
        )
    }

    private fun requestFields(requestMetadata: RequestMetadata): Map<String, Any> =
        buildMap {
            requestMetadata.requestId?.let { put("requestId", it) }
            requestMetadata.ipAddress?.let { put("ipAddress", it) }
        }

    private fun fingerprint(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(
            MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8)),
        )
}

interface SecurityEventSink {
    fun publish(entry: SecurityEventLogEntry)
}

data class SecurityEventLogEntry(
    val level: SecurityEventLevel,
    val event: String,
    val message: String,
    val fields: Map<String, Any>,
)

enum class SecurityEventLevel {
    INFO,
    WARN,
}

@Component
class Slf4jSecurityEventSink : SecurityEventSink {
    private val logger = LoggerFactory.getLogger(Slf4jSecurityEventSink::class.java)

    override fun publish(entry: SecurityEventLogEntry) {
        val loggingEvent = when (entry.level) {
            SecurityEventLevel.INFO -> logger.atInfo()
            SecurityEventLevel.WARN -> logger.atWarn()
        }

        loggingEvent
            .addKeyValue("event", entry.event)
            .also { event ->
                entry.fields.forEach { (key, value) -> event.addKeyValue(key, value) }
            }
            .log(entry.message)
    }
}
