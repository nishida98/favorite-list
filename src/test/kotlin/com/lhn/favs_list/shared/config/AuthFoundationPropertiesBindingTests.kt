package com.lhn.favs_list.shared.config

import kotlin.test.Test
import kotlin.test.assertEquals
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

class AuthFoundationPropertiesBindingTests {

    @Test
    fun `binds jwt and password foundation properties`() {
        val binder = Binder(
            MapConfigurationPropertySource(
                mapOf(
                    "app.auth.jwt.issuer" to "favs-list-api",
                    "app.auth.jwt.audience" to "favs-list-client",
                    "app.auth.jwt.access-token-ttl-seconds" to "900",
                    "app.auth.jwt.secret" to "test-secret",
                    "app.auth.password.algorithm" to "bcrypt",
                ),
            ),
        )

        val jwtProperties = binder.bind(
            "app.auth.jwt",
            Bindable.of(AuthJwtProperties::class.java),
        ).get()
        val passwordHashProperties = binder.bind(
            "app.auth.password",
            Bindable.of(PasswordHashProperties::class.java),
        ).get()

        assertEquals("favs-list-api", jwtProperties.issuer)
        assertEquals("favs-list-client", jwtProperties.audience)
        assertEquals(900L, jwtProperties.accessTokenTtlSeconds)
        assertEquals("test-secret", jwtProperties.secret)
        assertEquals(PasswordHashAlgorithm.BCRYPT, passwordHashProperties.algorithm)
    }
}
