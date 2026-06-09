package com.lhn.favs_list.users

import com.lhn.favs_list.sessions.persistence.UserLoginSessionEntity
import com.lhn.favs_list.sessions.persistence.UserLoginSessionStatus
import com.lhn.favs_list.shared.validation.InvalidInputException
import com.lhn.favs_list.shared.validation.UserInputValidator
import com.lhn.favs_list.testing.InMemoryUserLoginSessionRepository
import com.lhn.favs_list.testing.InMemoryUserRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class UserServiceTests {

    private val userRepository = InMemoryUserRepository()
    private val sessionRepository = InMemoryUserLoginSessionRepository()
    private val clock = Clock.fixed(Instant.parse("2026-06-08T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `returns the current authenticated user`() {
        val user = activeUser(
            id = UUID.fromString("1c7bf4d7-ebd1-492a-9aae-45ddf7d481d2"),
            email = "current@example.com",
            nickname = "current-user",
        )
        userRepository.seed(user)
        val userService = userService()

        val currentUser = userService.getCurrentUser(user.id)

        assertEquals(user.id, currentUser.id)
        assertEquals("current@example.com", currentUser.email)
    }

    @Test
    fun `updates the allowed profile fields and bumps updatedAt`() {
        val user = activeUser(
            id = UUID.fromString("95ad6828-d6ef-4d7b-b500-4dffefda3c2f"),
            email = "profile@example.com",
            nickname = "profile-user",
        )
        userRepository.seed(user)
        val userService = userService()

        val updatedUser = userService.updateCurrentUser(
            userId = user.id,
            command = UpdateCurrentUserCommand(
                name = "  Updated User  ",
                nickname = "  Updated.User  ",
            ),
        )

        assertEquals("Updated User", updatedUser.name)
        assertEquals("Updated.User", updatedUser.nickname)
        assertEquals(Instant.parse("2026-06-08T12:00:00Z"), updatedUser.updatedAt)
    }

    @Test
    fun `allows duplicate nicknames and rejects empty updates`() {
        val currentUser = activeUser(
            id = UUID.fromString("573fdd53-e973-4a93-b7cf-3d0bd08880c2"),
            email = "me@example.com",
            nickname = "me-user",
        )
        userRepository.seed(currentUser)
        val otherUser = activeUser(
            id = UUID.fromString("55f4aeed-c0ca-49b5-b0e8-c55d62af5f9d"),
            email = "other@example.com",
            nickname = "other-user",
        )
        userRepository.seed(otherUser)
        val userService = userService()

        val updatedUser = userService.updateCurrentUser(
            userId = currentUser.id,
            command = UpdateCurrentUserCommand(nickname = "Other-User"),
        )

        assertEquals("Other-User", updatedUser.nickname)
        assertFailsWith<InvalidInputException> {
            userService.updateCurrentUser(
                userId = currentUser.id,
                command = UpdateCurrentUserCommand(),
            )
        }
    }

    @Test
    fun `soft deletes the current user and revokes active sessions`() {
        val user = activeUser(
            id = UUID.fromString("355d4052-bd24-4e31-b922-4337c60f8f6b"),
            email = "delete@example.com",
            nickname = "delete-user",
        )
        userRepository.seed(user)
        sessionRepository.saveSuccessfulSession(
            UserLoginSessionEntity(
                id = UUID.fromString("228dc1a6-9c60-46b7-93d4-07a7ba896779"),
                userId = user.id,
                attemptedEmail = user.email,
                status = UserLoginSessionStatus.SUCCESS,
                tokenJti = "active-session",
                tokenHash = "token-hash",
                issuedAt = Instant.parse("2026-06-08T11:00:00Z"),
                expiresAt = Instant.parse("2026-06-08T13:00:00Z"),
                createdAt = Instant.parse("2026-06-08T11:00:00Z"),
                updatedAt = Instant.parse("2026-06-08T11:00:00Z"),
            ),
        )
        val userService = userService()

        val deleted = userService.deleteCurrentUser(user.id)
        val persistedUser = userRepository.findByNormalizedEmail(user.email)
        val revokedSession = sessionRepository.findByJti("active-session")

        assertEquals(true, deleted)
        assertNotNull(persistedUser?.deletedAt)
        assertEquals(Instant.parse("2026-06-08T12:00:00Z"), persistedUser?.updatedAt)
        assertEquals(UserLoginSessionStatus.REVOKED, revokedSession?.status)
    }

    private fun userService() =
        UserService(
            userRepository = userRepository,
            sessionRepository = sessionRepository,
            userInputValidator = UserInputValidator(),
            clock = clock,
        )

    private fun activeUser(
        id: UUID = UUID.randomUUID(),
        email: String,
        nickname: String,
    ) = com.lhn.favs_list.users.persistence.UserEntity(
        id = id,
        email = email,
        name = "Test User",
        nickname = nickname,
        passwordHash = "hash::Secret123",
        createdAt = Instant.parse("2026-06-08T10:00:00Z"),
        updatedAt = Instant.parse("2026-06-08T10:00:00Z"),
    )
}
