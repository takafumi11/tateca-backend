package com.moneyme.moneymebackend.repository;

import com.moneyme.moneymebackend.entity.ObligationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ObligationRepository extends JpaRepository<ObligationEntity, UUID> {
    @Query("SELECT o FROM ObligationEntity o WHERE o.loan.uuid = :loanId")
    List<ObligationEntity> findByLoanId(UUID loanId);
}
