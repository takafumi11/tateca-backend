package com.moneyme.moneymebackend.repository;

import com.moneyme.moneymebackend.entity.RepaymentEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepaymentRepository extends JpaRepository<RepaymentEntity, UUID> {
    @Query("SELECT r FROM RepaymentEntity r WHERE r.group.uuid = :groupId ORDER BY r.createdAt DESC")
    List<RepaymentEntity> getRepaymentsByGroup(UUID groupId, Pageable pageable);
}
