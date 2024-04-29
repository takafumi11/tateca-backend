package com.moneyme.moneymebackend.repository;

import com.moneyme.moneymebackend.entity.RepaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RepaymentRepository extends JpaRepository<RepaymentEntity, UUID> {
}
