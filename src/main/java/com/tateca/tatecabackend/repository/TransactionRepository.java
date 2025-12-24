package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.TransactionHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionHistoryEntity, UUID>, TransactionRepositoryCustom {
    Long countByGroup_Uuid(UUID groupUuid);
}
