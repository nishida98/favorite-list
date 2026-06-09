package com.lhn.favs_list.shared.config

import com.lhn.favs_list.shared.ids.UuidGenerator
import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SharedInfrastructureConfiguration {

    @Bean
    fun systemClock(): Clock = Clock.systemUTC()

    @Bean
    fun uuidGenerator(clock: Clock): UuidGenerator = UuidGenerator.uuidV7(clock)
}
