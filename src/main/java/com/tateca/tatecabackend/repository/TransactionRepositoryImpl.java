package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<TransactionHistoryEntity> findTransactionsByGroupWithLimit(UUID groupId, int limit) {
        return entityManager.createQuery("""
                SELECT t FROM TransactionHistoryEntity t
                JOIN FETCH t.payer p
                JOIN FETCH p.currencyName
                JOIN FETCH t.exchangeRate
                WHERE t.group.uuid = :groupId
                ORDER BY t.createdAt DESC
                """, TransactionHistoryEntity.class)
                .setParameter("groupId", groupId)
                .setMaxResults(limit)
                .getResultList();
    }
}
