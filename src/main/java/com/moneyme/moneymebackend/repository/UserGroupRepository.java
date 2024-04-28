package com.moneyme.moneymebackend.repository;

import com.moneyme.moneymebackend.entity.UserGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroupEntity, UUID> {
}
