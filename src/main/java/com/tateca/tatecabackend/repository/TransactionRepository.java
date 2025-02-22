package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import com.tateca.tatecabackend.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionHistoryEntity, UUID> {
    @Query(value = "SELECT * FROM transaction_history t WHERE t.group_uuid = :groupId ORDER BY t.transaction_date DESC LIMIT :limit", nativeQuery = true)
    List<TransactionHistoryEntity> findTransactionsByGroupOrderByCreatedAtDescWithLimit(@Param("groupId") UUID groupId, @Param("limit") int limit);
}
