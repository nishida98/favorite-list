package com.lhn.favs_list.auth

import com.lhn.favs_list.sessions.persistence.UserLoginSessionStatus
import com.lhn.favs_list.shared.logging.SecurityEventLogger
import com.lhn.favs_list.shared.validation.UserInputValidator
import com.lhn.favs_list.testing.InMemoryUserLoginSessionRepository
import com.lhn.favs_list.testing.InMemoryUserRepository
import com.lhn.favs_list.testing.QueueUuidGenerator
import com.lhn.favs_list.testing.RecordingSecurityEventSink
import com.lhn.favs_list.testing.StubPasswordHasher
import com.lhn.favs_list.testing.StubTokenService
import com.lhn.favs_list.users.EmailAlreadyInUseException
import com.lhn.favs_list.users.persistence.UserEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthServiceTests {

    private val userRepository = InMemoryUserRepository()
    private val sessionRepository = InMemoryUserLoginSessionRepository()
    private val passwordHasher = StubPasswordHasher()
    private val securityEventSink = RecordingSecurityEventSink()
    private val securityEventLogger = SecurityEventLogger(securityEventSink)
    private val tokenService = StubTokenService(
        issuedAt = Instant.parse("2026-06-08T12:00:00Z"),
        expiresAt = Instant.parse("2026-06-08T12:15:00Z"),
    )
    private val clock = Clock.fixed(Instant.parse("2026-06-08T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `registers a user with normalized data and hashed password`() {
        val userId = UUID.fromString("e82f7158-f935-4861-bb83-bf3e606c9050")
        val authService = authService(
            uuidGenerator = QueueUuidGenerator(userId),
        )

        val registeredUser = authService.registerUser(
            RegisterUserCommand(
                email = "  Test.User@Example.COM ",
                name = "  Test User  ",
                nickname = "  Test.User  ",
                password = "Secret123",
            ),
        )

        assertEquals(userId, registeredUser.id)
        assertEquals("test.user@example.com", registeredUser.email)
        assertEquals("Test User", registeredUser.name)
        assertEquals("Test.User", registeredUser.nickname)
        assertEquals("hash::Secret123", userRepository.findByNormalizedEmail("test.user@example.com")?.passwordHash)
        assertEquals("auth_register_success", securityEventSink.entries.single().event)
    }

    @Test
    fun `rejects duplicate email during registration while allowing duplicate nicknames`() {
        userRepository.seed(activeUser(email = "taken@example.com", nickname = "TakenNick"))
        val duplicateNicknameUserId = UUID.fromString("b4d4c718-f69c-433f-a5c4-94a228f2f2d7")
        val authService = authService(
            uuidGenerator = QueueUuidGenerator(duplicateNicknameUserId),
        )

        assertFailsWith<EmailAlreadyInUseException> {
            authService.registerUser(
                RegisterUserCommand(
                    email = "taken@example.com",
                    name = "Test User",
                    nickname = "AnotherNick",
                    password = "Secret123",
                ),
            )
        }

        val registeredUser = authService.registerUser(
            RegisterUserCommand(
                email = "other@example.com",
                name = "Test User",
                nickname = "takennick",
                password = "Secret123",
            ),
        )

        assertEquals(duplicateNicknameUserId, registeredUser.id)
        assertEquals("takennick", registeredUser.nickname)
        assertEquals("auth_register_failure", securityEventSink.entries.first().event)
    }

    @Test
    fun `records failed login attempts for unknown or deleted users and bad passwords`() {
        val deletedUser = activeUser(
            id = UUID.fromString("e7a84f80-c1dd-43f0-b909-9f923c4df62f"),
            email = "deleted@example.com",
            nickname = "deleted-user",
        ).also { it.deletedAt = Instant.parse("2026-06-08T10:00:00Z") }
        val activeUser = activeUser(
            id = UUID.fromString("611ce568-bf27-4cb9-9101-0ddbeb480d15"),
            email = "active@example.com",
            nickname = "active-user",
        )
        userRepository.seed(deletedUser)
        userRepository.seed(activeUser)
        val authService = authService(
            uuidGenerator = QueueUuidGenerator(
                UUID.fromString("a9ca2a8f-e8e9-420d-9559-bbf555718e78"),
                UUID.fromString("c8091140-f8e3-4849-87d5-4801e340af61"),
                UUID.fromString("93cbb74d-cbaa-4d91-a5f3-0c62d335c60e"),
            ),
        )

        assertFailsWith<AuthenticationFailedException> {
            authService.login(LoginCommand(email = "missing@example.com", password = "Secret123"))
        }
        assertFailsWith<AuthenticationFailedException> {
            authService.login(LoginCommand(email = "deleted@example.com", password = "Secret123"))
        }
        assertFailsWith<AuthenticationFailedException> {
            authService.login(LoginCommand(email = "active@example.com", password = "Wrong1234"))
        }

        assertEquals(3, sessionRepository.failedAttempts.size)
        assertEquals(LoginFailureReason.USER_NOT_FOUND.name, sessionRepository.failedAttempts[0].failureReason)
        assertNull(sessionRepository.failedAttempts[0].userId)
        assertEquals(LoginFailureReason.USER_DELETED.name, sessionRepository.failedAttempts[1].failureReason)
        assertEquals(deletedUser.id, sessionRepository.failedAttempts[1].userId)
        assertEquals(LoginFailureReason.INVALID_PASSWORD.name, sessionRepository.failedAttempts[2].failureReason)
        assertEquals(activeUser.id, sessionRepository.failedAttempts[2].userId)
        assertEquals(
            listOf("auth_login_failure", "auth_login_failure", "auth_login_failure"),
            securityEventSink.entries.map { it.event },
        )
    }

    @Test
    fun `creates a successful session before returning the login result`() {
        val user = activeUser(
            id = UUID.fromString("0be7fd6d-c2e8-405b-a85c-4217d75cd1ec"),
            email = "login@example.com",
            nickname = "login-user",
        )
        userRepository.seed(user)
        val tokenJti = UUID.fromString("4c2f16b6-28de-4afd-afda-e38f15465044")
        val sessionId = UUID.fromString("27b2e433-5ef7-4fb2-b7a3-4cc0ebc4754a")
        val authService = authService(
            uuidGenerator = QueueUuidGenerator(tokenJti, sessionId),
        )

        val loginResult = authService.login(
            command = LoginCommand(email = "login@example.com", password = "Secret123"),
            requestMetadata = RequestMetadata(
                ipAddress = "127.0.0.1",
                userAgent = "JUnit",
            ),
        )

        val persistedSession = sessionRepository.findByJti(tokenJti.toString())

        assertEquals("token-for-${tokenJti}", loginResult.accessToken)
        assertEquals("Bearer", loginResult.tokenType)
        assertEquals(900L, loginResult.expiresIn)
        assertNotNull(persistedSession)
        assertEquals(sessionId, persistedSession.id)
        assertEquals(UserLoginSessionStatus.SUCCESS, persistedSession.status)
        assertEquals(user.id, persistedSession.userId)
        assertEquals("127.0.0.1", persistedSession.ipAddress)
        assertEquals("JUnit", persistedSession.userAgent)
        assertTrue(persistedSession.tokenHash?.isNotBlank() == true)
        assertEquals("auth_login_success", securityEventSink.entries.single().event)
    }

    @Test
    fun `logout revokes the current session`() {
        val user = activeUser(
            id = UUID.fromString("f10b9235-4f5d-4aab-86f1-fcf63500af82"),
            email = "logout@example.com",
            nickname = "logout-user",
        )
        userRepository.seed(user)
        val tokenJti = UUID.fromString("be1d4fcf-9973-4ba8-ba0b-cb3645393302")
        val sessionId = UUID.fromString("75c4a1e0-bbd5-4edf-a34c-2628344a0d87")
        val authService = authService(
            uuidGenerator = QueueUuidGenerator(tokenJti, sessionId),
        )

        authService.login(LoginCommand(email = "logout@example.com", password = "Secret123"))
        val revoked = authService.logout(user.id, tokenJti.toString())

        assertTrue(revoked)
        assertEquals(UserLoginSessionStatus.REVOKED, sessionRepository.findByJti(tokenJti.toString())?.status)
        assertEquals(
            listOf("auth_login_success", "auth_logout_success"),
            securityEventSink.entries.map { it.event },
        )
    }

    private fun authService(uuidGenerator: QueueUuidGenerator): AuthService =
        AuthService(
            userRepository = userRepository,
            sessionRepository = sessionRepository,
            userInputValidator = UserInputValidator(),
            passwordHasher = passwordHasher,
            tokenService = tokenService,
            uuidGenerator = uuidGenerator,
            clock = clock,
            securityEventLogger = securityEventLogger,
        )

    private fun activeUser(
        id: UUID = UUID.randomUUID(),
        email: String,
        nickname: String,
    ) = UserEntity(
        id = id,
        email = email,
        name = "Test User",
        nickname = nickname,
        passwordHash = "hash::Secret123",
        createdAt = Instant.parse("2026-06-08T10:00:00Z"),
        updatedAt = Instant.parse("2026-06-08T10:00:00Z"),
    )
}
