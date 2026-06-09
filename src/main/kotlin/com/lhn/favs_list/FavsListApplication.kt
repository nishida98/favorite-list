package com.lhn.favs_list

import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class FavsListApplication

fun main(args: Array<String>) {
	runApplication<FavsListApplication>(*args)
}
