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
                    "app.auth.jwt.secret" to "test-secret-value-12345678901234567890",
                    "app.auth.password.algorithm" to "bcrypt",
                    "app.cors.allowed-origins[0]" to "https://app.example.com",
                    "app.graphql.security.max-query-depth" to "8",
                    "app.graphql.security.max-query-complexity" to "100",
                    "app.graphql.security.max-request-body-bytes" to "1048576",
                    "app.auth.rate-limit.login-per-minute" to "5",
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
        val corsProperties = binder.bind(
            "app.cors",
            Bindable.of(CorsAllowedOriginsProperties::class.java),
        ).get()
        val graphqlSecurityProperties = binder.bind(
            "app.graphql.security",
            Bindable.of(GraphqlSecurityProperties::class.java),
        ).get()
        val authRateLimitProperties = binder.bind(
            "app.auth.rate-limit",
            Bindable.of(AuthRateLimitProperties::class.java),
        ).get()

        assertEquals("favs-list-api", jwtProperties.issuer)
        assertEquals("favs-list-client", jwtProperties.audience)
        assertEquals(900L, jwtProperties.accessTokenTtlSeconds)
        assertEquals("test-secret-value-12345678901234567890", jwtProperties.secret)
        assertEquals(PasswordHashAlgorithm.BCRYPT, passwordHashProperties.algorithm)
        assertEquals(listOf("https://app.example.com"), corsProperties.allowedOrigins)
        assertEquals(8, graphqlSecurityProperties.maxQueryDepth)
        assertEquals(100, graphqlSecurityProperties.maxQueryComplexity)
        assertEquals(1048576L, graphqlSecurityProperties.maxRequestBodyBytes)
        assertEquals(5, authRateLimitProperties.loginPerMinute)
    }
}
