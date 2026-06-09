package com.lhn.favs_list.users.persistence

import com.lhn.favs_list.shared.testing.PostgreSqlRepositoryTestSupport
import com.lhn.favs_list.users.UserRepository
import jakarta.persistence.EntityManager
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(
    properties = [
        "spring.jpa.hibernate.ddl-auto=validate",
        "app.auth.jwt.secret=test-secret-value-12345678901234567890",
        "app.cors.allowed-origins[0]=https://app.example.test",
    ],
)
@Transactional
class JpaUserRepositoryTests : PostgreSqlRepositoryTestSupport() {

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `finds active users by id and normalized email while excluding soft deleted users`() {
        val activeUser = userEntity(
            email = "active@example.com",
            nickname = "ActiveOne",
        )
        val deletedUser = userEntity(
            email = "deleted@example.com",
            nickname = "DeletedOne",
            deletedAt = Instant.parse("2026-06-08T12:30:00Z"),
        )

        userRepository.save(activeUser)
        userRepository.save(deletedUser)

        val foundById = userRepository.findActiveById(activeUser.id)
        val foundByEmail = userRepository.findActiveByNormalizedEmail(activeUser.email)

        assertNotNull(foundById)
        assertEquals(activeUser.id, foundById.id)
        assertNotNull(foundByEmail)
        assertEquals(activeUser.id, foundByEmail.id)
        assertNull(userRepository.findActiveById(deletedUser.id))
        assertNull(userRepository.findActiveByNormalizedEmail(deletedUser.email))
    }

    @Test
    fun `reserves normalized email without nickname uniqueness checks`() {
        userRepository.save(
            userEntity(
                email = "saved@example.com",
                nickname = "CaseMix",
            ),
        )

        assertTrue(userRepository.existsByNormalizedEmail("saved@example.com"))
    }

    @Test
    fun `keeps email reserved after soft delete`() {
        val deletedUser = userEntity(
            email = "reserved@example.com",
            nickname = "ReservedNick",
            deletedAt = Instant.parse("2026-06-08T13:00:00Z"),
        )

        userRepository.save(deletedUser)

        assertTrue(userRepository.existsByNormalizedEmail(deletedUser.email))
    }

    @Test
    fun `allows duplicate nicknames in the database`() {
        userRepository.save(
            userEntity(
                email = "first@example.com",
                nickname = "NickName",
            ),
        )
        userRepository.save(
            userEntity(
                email = "second@example.com",
                nickname = "nickname",
            ),
        )
        entityManager.flush()
    }

    @Test
    fun `updates persisted users`() {
        val savedUser = userRepository.save(
            userEntity(
                email = "update@example.com",
                nickname = "UpdateNick",
                name = "Before Update",
            ),
        )
        savedUser.name = "After Update"
        savedUser.nickname = "UpdatedNick"
        savedUser.updatedAt = Instant.parse("2026-06-08T15:45:00Z")

        userRepository.update(savedUser)
        entityManager.clear()

        val reloadedUser = userRepository.findActiveById(savedUser.id)

        assertNotNull(reloadedUser)
        assertEquals("After Update", reloadedUser.name)
        assertEquals("UpdatedNick", reloadedUser.nickname)
        assertEquals(Instant.parse("2026-06-08T15:45:00Z"), reloadedUser.updatedAt)
    }

    private fun userEntity(
        email: String,
        nickname: String,
        name: String = "Test User",
        deletedAt: Instant? = null,
    ): UserEntity =
        UserEntity(
            id = UUID.randomUUID(),
            email = email,
            name = name,
            nickname = nickname,
            passwordHash = "argon2id-hash",
            createdAt = Instant.parse("2026-06-08T12:00:00Z"),
            updatedAt = Instant.parse("2026-06-08T12:00:00Z"),
            deletedAt = deletedAt,
        )
}
