package com.moneyme.moneymebackend.repository;

import com.moneyme.moneymebackend.entity.UserGroupEntity;
import com.moneyme.moneymebackend.entity.UserGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroupEntity, UserGroupId> {
    @Query("SELECT uge FROM UserGroupEntity uge WHERE uge.groupUuid = :groupUuid")
    List<UserGroupEntity> findByGroupUuid(UUID groupUuid);
}
