package com.lhn.favs_list.shared.config

import jakarta.validation.constraints.Min
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties("app.graphql.security")
data class GraphqlSecurityProperties(
    @field:Min(1)
    val maxQueryDepth: Int,
    @field:Min(1)
    val maxQueryComplexity: Int,
    @field:Min(1)
    val maxRequestBodyBytes: Long,
    val allowBatchRequests: Boolean = false,
)
