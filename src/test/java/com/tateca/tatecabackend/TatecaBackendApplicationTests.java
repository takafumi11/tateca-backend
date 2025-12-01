package com.tateca.tatecabackend;

import com.tateca.tatecabackend.service.AbstractServiceIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic integration test to verify Spring Boot application context loads correctly.
 *
 * <p>This test uses AssertJ for all assertions as per project testing standards.</p>
 */
class TatecaBackendApplicationTests extends AbstractServiceIntegrationTest {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	@DisplayName("Spring Boot application context should load successfully")
	void contextLoads() {
		assertThat(applicationContext).isNotNull();
	}

	@Test
	@DisplayName("MySQL container should be running")
	void mysqlContainerIsRunning() {
		assertThat(mysql.isRunning()).isTrue();
	}

}
