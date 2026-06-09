package com.lhn.favs_list.users.persistence

import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SpringDataUserEntityRepository : JpaRepository<UserEntity, UUID> {
    fun findByEmail(email: String): UserEntity?

    @Query(
        """
        select user
        from UserEntity user
        where user.id = :id
          and user.deletedAt is null
        """,
    )
    fun findActiveById(@Param("id") id: UUID): UserEntity?

    @Query(
        """
        select user
        from UserEntity user
        where user.email = :email
          and user.deletedAt is null
        """,
    )
    fun findActiveByEmail(@Param("email") email: String): UserEntity?

    fun existsByEmail(email: String): Boolean
}
