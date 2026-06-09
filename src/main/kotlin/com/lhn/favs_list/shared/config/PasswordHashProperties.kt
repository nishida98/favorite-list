package com.lhn.favs_list.shared.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("app.auth.password")
data class PasswordHashProperties(
    val algorithm: PasswordHashAlgorithm = PasswordHashAlgorithm.ARGON2ID,
)

enum class PasswordHashAlgorithm {
    ARGON2ID,
    BCRYPT,
}
