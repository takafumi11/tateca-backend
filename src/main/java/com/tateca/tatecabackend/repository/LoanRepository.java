package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.LoanEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LoanRepository extends JpaRepository<LoanEntity, UUID> {
    @Query("SELECT l FROM LoanEntity l WHERE l.group.uuid = :groupId ORDER BY l.createdAt DESC")
    List<LoanEntity> getLoansByGroup(UUID groupId, Pageable pageable);

    @Query("SELECT r FROM LoanEntity r WHERE r.group.uuid = :groupId")
    List<LoanEntity> findByGroupId(UUID groupId);
}
