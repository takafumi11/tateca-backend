package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.TransactionObligationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ObligationRepository extends JpaRepository<TransactionObligationEntity, UUID> {
    @Query("SELECT o FROM TransactionObligationEntity o WHERE o.transaction.uuid = :loanId")
    List<TransactionObligationEntity> findByTransactionId(UUID loanId);

    @Query("""
            SELECT DISTINCT o
            FROM TransactionObligationEntity o
            JOIN FETCH o.transaction t
            JOIN FETCH o.user
            JOIN FETCH t.payer
            JOIN FETCH t.exchangeRate
            WHERE t.group.uuid = :groupId
            """)
    List<TransactionObligationEntity> findByGroupId(UUID groupId);
}
