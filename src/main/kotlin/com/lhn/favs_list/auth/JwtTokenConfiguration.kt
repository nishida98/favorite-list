package com.lhn.favs_list.auth

import com.lhn.favs_list.shared.config.AuthJwtProperties
import java.nio.charset.StandardCharsets
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateCrtKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.RSAPublicKeySpec
import java.time.Clock
import java.time.Duration
import javax.crypto.spec.SecretKeySpec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.converter.RsaKeyConverters
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtClaimValidator
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtTimestampValidator
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm

@Configuration
class JwtTokenConfiguration {

    @Bean
    fun jwtEncoder(authJwtProperties: AuthJwtProperties): JwtEncoder =
        if (authJwtProperties.usesSharedSecret()) {
            NimbusJwtEncoder.withSecretKey(sharedSecretKey(authJwtProperties))
                .algorithm(MacAlgorithm.HS256)
                .build()
        } else {
            val privateKey = privateKey(authJwtProperties)
            NimbusJwtEncoder.withKeyPair(publicKey(privateKey), privateKey)
                .algorithm(SignatureAlgorithm.RS256)
                .build()
        }

    @Bean
    fun jwtDecoder(
        authJwtProperties: AuthJwtProperties,
        clock: Clock,
    ): JwtDecoder {
        val decoder = if (authJwtProperties.usesSharedSecret()) {
            NimbusJwtDecoder.withSecretKey(sharedSecretKey(authJwtProperties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build()
        } else {
            NimbusJwtDecoder.withPublicKey(publicKey(privateKey(authJwtProperties)))
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build()
        }

        decoder.setJwtValidator(jwtValidator(authJwtProperties, clock))

        return decoder
    }

    @Bean
    fun tokenService(
        jwtEncoder: JwtEncoder,
        jwtDecoder: JwtDecoder,
        authJwtProperties: AuthJwtProperties,
        clock: java.time.Clock,
    ): TokenService =
        JwtTokenService(
            jwtEncoder = jwtEncoder,
            jwtDecoder = jwtDecoder,
            authJwtProperties = authJwtProperties,
            clock = clock,
        )

    private fun sharedSecretKey(authJwtProperties: AuthJwtProperties) =
        SecretKeySpec(
            authJwtProperties.secret!!.toByteArray(StandardCharsets.UTF_8),
            "HmacSHA256",
        )

    private fun privateKey(authJwtProperties: AuthJwtProperties): RSAPrivateCrtKey =
        RsaKeyConverters.pkcs8().convert(
            ByteArrayInputStream(normalizePem(authJwtProperties.privateKey!!).toByteArray(StandardCharsets.UTF_8)),
        ) as? RSAPrivateCrtKey
            ?: throw IllegalStateException("JWT private key must be a PKCS#8 RSA private key")

    private fun publicKey(privateKey: RSAPrivateCrtKey): RSAPublicKey =
        KeyFactory.getInstance("RSA")
            .generatePublic(RSAPublicKeySpec(privateKey.modulus, privateKey.publicExponent)) as RSAPublicKey

    private fun normalizePem(value: String): String =
        value.replace("\\n", "\n")

    private fun jwtValidator(
        authJwtProperties: AuthJwtProperties,
        clock: Clock,
    ): OAuth2TokenValidator<Jwt> {
        val timestampValidator = JwtTimestampValidator(Duration.ZERO).apply {
            setClock(clock)
        }
        val issuerValidator = JwtClaimValidator<String>("iss") { issuer ->
            issuer == authJwtProperties.issuer
        }
        val audienceValidator = JwtClaimValidator<Collection<String>>("aud") { audience ->
            audience?.contains(authJwtProperties.audience) == true
        }

        return DelegatingOAuth2TokenValidator(timestampValidator, issuerValidator, audienceValidator)
    }
}
