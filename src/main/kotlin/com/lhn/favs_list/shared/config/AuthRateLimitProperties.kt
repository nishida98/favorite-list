package com.lhn.favs_list.shared.config

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("app.auth.rate-limit")
data class AuthRateLimitProperties(
    @field:Min(1)
    val loginPerMinute: Int? = null,
)
