package com.lhn.favs_list.auth

import com.lhn.favs_list.shared.config.AuthJwtProperties
import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import java.time.Clock
import java.time.Duration
import java.nio.charset.StandardCharsets
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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

@Configuration
class JwtTokenConfiguration {

    @Bean
    fun jwtEncoder(authJwtProperties: AuthJwtProperties): JwtEncoder =
        NimbusJwtEncoder(ImmutableSecret<SecurityContext>(secretKey(authJwtProperties)))

    @Bean
    fun jwtDecoder(
        authJwtProperties: AuthJwtProperties,
        clock: Clock,
    ): JwtDecoder {
        val decoder = NimbusJwtDecoder.withSecretKey(secretKey(authJwtProperties))
            .macAlgorithm(MacAlgorithm.HS256)
            .build()

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

    private fun secretKey(authJwtProperties: AuthJwtProperties): SecretKey =
        SecretKeySpec(
            authJwtProperties.secret.toByteArray(StandardCharsets.UTF_8),
            "HmacSHA256",
        )

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
