package com.moneyme.moneymebackend.repository;

import com.moneyme.moneymebackend.entity.LoanObligationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoanObligationRepository extends JpaRepository<LoanObligationEntity, UUID> {
}
