package com.lhn.favs_list.auth

import com.lhn.favs_list.shared.config.PasswordHashAlgorithm
import com.lhn.favs_list.shared.config.PasswordHashProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration
class PasswordHashConfiguration {

    @Bean
    fun passwordHasher(passwordHashProperties: PasswordHashProperties): PasswordHasher {
        val encoder = when (passwordHashProperties.algorithm) {
            PasswordHashAlgorithm.ARGON2ID -> Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
            PasswordHashAlgorithm.BCRYPT -> BCryptPasswordEncoder(12)
        }

        return SpringSecurityPasswordHasher(encoder)
    }
}

private class SpringSecurityPasswordHasher(
    private val passwordEncoder: PasswordEncoder,
) : PasswordHasher {

    override fun hash(rawPassword: String): String =
        requireNotNull(passwordEncoder.encode(rawPassword)) {
            "Password encoder returned a null hash"
        }

    override fun matches(rawPassword: String, passwordHash: String): Boolean =
        passwordEncoder.matches(rawPassword, passwordHash)
}
