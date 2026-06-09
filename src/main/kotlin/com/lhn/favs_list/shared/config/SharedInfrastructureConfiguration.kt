package com.lhn.favs_list.shared.config

import com.lhn.favs_list.shared.ids.UuidGenerator
import java.time.Clock
import java.util.UUID
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SharedInfrastructureConfiguration {

    @Bean
    fun systemClock(): Clock = Clock.systemUTC()

    @Bean
    fun uuidGenerator(): UuidGenerator = UuidGenerator(UUID::randomUUID)
}
