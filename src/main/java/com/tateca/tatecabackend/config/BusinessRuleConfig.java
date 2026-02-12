package com.tateca.tatecabackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for business rules and constraints.
 *
 * <p>These settings control business logic behavior such as:
 * <ul>
 *   <li>User privilege exceptions (unlimited group membership)</li>
 *   <li>Special account handling</li>
 * </ul>
 *
 * <p>Configuration is loaded from application.properties with prefix "tateca.business"
 */
@Configuration
@ConfigurationProperties(prefix = "tateca.business")
@Data
public class BusinessRuleConfig {

    /**
     * UID that bypasses the maximum group count limit.
     *
     * <p>This is typically used for:
     * <ul>
     *   <li>Admin accounts that need to manage many groups</li>
     *   <li>Test accounts in development/staging environments</li>
     *   <li>Special service accounts</li>
     * </ul>
     *
     * <p>Configure via environment variable: {@code UNLIMITED_GROUP_UID}
     *
     * <p>Default: dev-unlimited-uid (for local development)
     * <p>Production: Set via environment variable UNLIMITED_GROUP_UID
     */
    private String unlimitedGroupUid;
}
