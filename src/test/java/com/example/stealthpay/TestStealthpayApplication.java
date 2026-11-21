package com.example.stealthpay;

import org.springframework.boot.SpringApplication;

public class TestStealthpayApplication {

	public static void main(String[] args) {
		SpringApplication.from(StealthpayApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
