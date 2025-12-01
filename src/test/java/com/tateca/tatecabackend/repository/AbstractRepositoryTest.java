package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.config.TestConfig;
import org.flywaydb.core.Flyway;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for Repository tests using @DataJpaTest with Testcontainers MySQL.
 * Tests JPA repositories with real MySQL 8.0 database and Flyway migrations.
 */
@DataJpaTest(
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Flyway.class),
        properties = {
                "spring.flyway.enabled=true"
        }
)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(TestConfig.class)
@SuppressWarnings("resource") // MySQLContainer lifecycle is managed by @Container and @Testcontainers
public abstract class AbstractRepositoryTest {

    @Container
    @ServiceConnection
    protected static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpass")
            .withReuse(true);

    static {
        mysql.start();
    }
}
