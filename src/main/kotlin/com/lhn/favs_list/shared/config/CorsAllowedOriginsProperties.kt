package com.lhn.favs_list.shared.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("app.cors")
data class CorsAllowedOriginsProperties(
    @field:Size(min = 1, message = "At least one CORS allowed origin must be configured")
    val allowedOrigins: List<@NotBlank String>,
)
