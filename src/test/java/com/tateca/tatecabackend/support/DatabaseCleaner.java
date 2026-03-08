package com.tateca.tatecabackend.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Deletes all rows from application tables between tests for MockMvc-based integration tests.
 *
 * <p>MockMvc tests run HTTP requests that commit their own transactions,
 * so Spring's test @Transactional rollback cannot clean up after them.
 * Call {@link #clean()} in @BeforeEach to guarantee test isolation.
 *
 * <p>Uses DELETE (DML) instead of TRUNCATE (DDL) to avoid invalidating
 * concurrent transactions' table metadata in MySQL InnoDB.
 * Tables are deleted in FK-dependency order (children first).
 */
@Component
public class DatabaseCleaner {

    private static final List<String> TABLES_IN_DELETE_ORDER = List.of(
            "transaction_obligations",
            "transaction_history",
            "user_groups",
            "users",
            "exchange_rates",
            "`groups`",
            "auth_users",
            "currencies"
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void clean() {
        entityManager.flush();
        for (String table : TABLES_IN_DELETE_ORDER) {
            entityManager.createNativeQuery("DELETE FROM " + table).executeUpdate();
        }
    }
}
