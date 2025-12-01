package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.config.AbstractIntegrationTest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base class for Service integration tests.
 * Provides transactional testing with automatic rollback and EntityManager helper methods.
 */
@Transactional
public abstract class AbstractServiceIntegrationTest extends AbstractIntegrationTest {

    @PersistenceContext
    protected EntityManager entityManager;

    /**
     * Flushes pending changes to the database.
     */
    protected void flush() {
        entityManager.flush();
    }

    /**
     * Clears the persistence context, detaching all entities.
     */
    protected void clear() {
        entityManager.clear();
    }

    /**
     * Flushes and clears the persistence context.
     */
    protected void flushAndClear() {
        flush();
        clear();
    }
}
