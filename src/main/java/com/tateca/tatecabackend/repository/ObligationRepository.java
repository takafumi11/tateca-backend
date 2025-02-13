package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.ObligationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ObligationRepository extends JpaRepository<ObligationEntity, UUID> {
    @Query("SELECT o FROM ObligationEntity o WHERE o.transaction.uuid = :loanId")
    List<ObligationEntity> findByTransactionId(UUID loanId);

    @Query("SELECT o FROM ObligationEntity o WHERE o.transaction.group.uuid = :groupId")
    List<ObligationEntity> findByGroupId(UUID groupId);
}
