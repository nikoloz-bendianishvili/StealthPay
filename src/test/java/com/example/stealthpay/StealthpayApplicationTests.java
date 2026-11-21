package com.example.stealthpay;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/** Starts the whole app: proves the Spring wiring, Flyway V1 and Hibernate "validate" all agree. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class StealthpayApplicationTests {

	@Test
	void contextLoads() {
	}

}
