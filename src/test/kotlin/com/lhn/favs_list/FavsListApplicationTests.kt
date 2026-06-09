package com.lhn.favs_list

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "app.auth.jwt.secret=test-secret-value-12345678901234567890",
        "app.cors.allowed-origins[0]=https://app.example.test",
    ],
)
class FavsListApplicationTests {

	@Test
	fun contextLoads() {
	}

}
