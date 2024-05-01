package com.moneyme.moneymebackend.repository;

import com.moneyme.moneymebackend.entity.GroupEntity;
import com.moneyme.moneymebackend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, UUID> {
}
