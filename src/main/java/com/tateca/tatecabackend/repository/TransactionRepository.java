package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    @Query(value = "SELECT * FROM transaction_history t WHERE t.group_uuid = :groupId ORDER BY t.created_at DESC LIMIT :limit", nativeQuery = true)
    List<TransactionEntity> findTransactionsByGroupOrderByCreatedAtDesc(@Param("groupId") UUID groupId, @Param("limit") int limit);
}
