package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.TransactionHistoryEntity;

import java.util.List;
import java.util.UUID;

public interface TransactionRepositoryCustom {
    /**
     * Find transactions by group with JOIN FETCH for related entities (payer, exchangeRate)
     * Uses setMaxResults() instead of Pageable to avoid COUNT query
     */
    List<TransactionHistoryEntity> findTransactionsByGroupWithLimit(UUID groupId, int limit);
}
