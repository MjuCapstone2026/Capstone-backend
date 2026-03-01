package com.mju.capstone_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class CapstoneBackendApplication {

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(CapstoneBackendApplication.class, args);

		// String dbUrl = context.getEnvironment().getProperty("DB_URL");
		// System.out.println("연결된 DB URL: " + dbUrl);
	}

}
