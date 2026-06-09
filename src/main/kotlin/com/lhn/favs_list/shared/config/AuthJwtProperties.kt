package com.lhn.favs_list.shared.config

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
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
    @field:NotBlank
    val secret: String,
)
