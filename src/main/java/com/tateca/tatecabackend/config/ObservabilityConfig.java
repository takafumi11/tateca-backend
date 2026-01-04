package com.tateca.tatecabackend.config;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.sql.DataSource;

/**
 * Configuration for database observability and monitoring.
 *
 * <p>Features:
 * <ul>
 *   <li>Hibernate statistics logging (development only)</li>
 *   <li>Slow query detection (> 1 second)</li>
 *   <li>Connection pool monitoring (HikariCP)</li>
 * </ul>
 *
 * <p>This configuration is conditionally enabled based on the active Spring profile.
 * In production, statistics are disabled to reduce overhead.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "spring.profiles.active", havingValue = "dev", matchIfMissing = true)
public class ObservabilityConfig {
    private static final Logger logger = LoggerFactory.getLogger(ObservabilityConfig.class);

    private final EntityManagerFactory entityManagerFactory;
    private final DataSource dataSource;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public ObservabilityConfig(EntityManagerFactory entityManagerFactory, DataSource dataSource) {
        this.entityManagerFactory = entityManagerFactory;
        this.dataSource = dataSource;
    }

    /**
     * Log database statistics every 5 minutes (development only).
     *
     * <p>Metrics logged:
     * <ul>
     *   <li>Query count</li>
     *   <li>Query execution time</li>
     *   <li>Cache hit/miss ratio</li>
     *   <li>Connection pool usage</li>
     * </ul>
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    public void logDatabaseStatistics() {
        if (!"dev".equals(activeProfile)) {
            return; // Only log in development
        }

        try {
            // Hibernate statistics
            SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
            Statistics stats = sessionFactory.getStatistics();

            if (stats.isStatisticsEnabled()) {
                logger.info("Hibernate Statistics: queries={}, queryExecutionMaxTime={}ms, " +
                                "secondLevelCacheHitCount={}, secondLevelCacheMissCount={}, " +
                                "sessionOpenCount={}, sessionCloseCount={}",
                        stats.getQueryExecutionCount(),
                        stats.getQueryExecutionMaxTime(),
                        stats.getSecondLevelCacheHitCount(),
                        stats.getSecondLevelCacheMissCount(),
                        stats.getSessionOpenCount(),
                        stats.getSessionCloseCount());

                // Slow query detection
                if (stats.getQueryExecutionMaxTime() > 1000) {
                    logger.warn("Slow query detected: maxExecutionTime={}ms (threshold: 1000ms)",
                            stats.getQueryExecutionMaxTime());
                }
            }

            // HikariCP connection pool statistics
            if (dataSource instanceof HikariDataSource hikariDataSource) {
                logger.info("HikariCP Pool Statistics: active={}, idle={}, waiting={}, total={}",
                        hikariDataSource.getHikariPoolMXBean().getActiveConnections(),
                        hikariDataSource.getHikariPoolMXBean().getIdleConnections(),
                        hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection(),
                        hikariDataSource.getHikariPoolMXBean().getTotalConnections());

                // Warn if connection pool is exhausted
                int activeConnections = hikariDataSource.getHikariPoolMXBean().getActiveConnections();
                int maxPoolSize = hikariDataSource.getMaximumPoolSize();
                if (activeConnections >= maxPoolSize * 0.9) {
                    logger.warn("Connection pool near exhaustion: active={}, max={}, utilization={}%",
                            activeConnections, maxPoolSize, (activeConnections * 100 / maxPoolSize));
                }
            }
        } catch (Exception e) {
            logger.error("Failed to log database statistics: {}", e.getMessage(), e);
        }
    }

    /**
     * Enable Hibernate statistics on startup (development only).
     */
    @Bean
    public Statistics hibernateStatistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();

        if ("dev".equals(activeProfile)) {
            stats.setStatisticsEnabled(true);
            logger.info("Hibernate statistics enabled for development environment");
        } else {
            stats.setStatisticsEnabled(false);
            logger.info("Hibernate statistics disabled for production environment");
        }

        return stats;
    }
}
