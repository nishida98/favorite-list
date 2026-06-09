package com.lhn.favs_list.shared.config

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.oauth2.jose.jws.JwsAlgorithm
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("app.auth.jwt")
data class AuthJwtProperties(
    @field:NotBlank
    val issuer: String,
    @field:NotBlank
    val audience: String,
    @field:Min(1)
    val accessTokenTtlSeconds: Long,
    val secret: String? = null,
    val privateKey: String? = null,
) {

    @AssertTrue(message = "Exactly one of app.auth.jwt.secret or app.auth.jwt.private-key must be configured")
    fun hasSingleSigningKeySource(): Boolean =
        secret.isConfigured() xor privateKey.isConfigured()

    @AssertTrue(message = "JWT shared secrets must be at least 32 characters long")
    fun sharedSecretIsStrongEnough(): Boolean =
        !secret.isConfigured() || secret!!.length >= MINIMUM_SECRET_LENGTH

    fun usesSharedSecret(): Boolean = secret.isConfigured()

    fun usesPrivateKey(): Boolean = privateKey.isConfigured()

    fun jwsAlgorithm(): JwsAlgorithm =
        if (usesSharedSecret()) {
            MacAlgorithm.HS256
        } else {
            SignatureAlgorithm.RS256
        }

    companion object {
        const val MINIMUM_SECRET_LENGTH = 32
    }
}

private fun String?.isConfigured(): Boolean =
    !this.isNullOrBlank()
