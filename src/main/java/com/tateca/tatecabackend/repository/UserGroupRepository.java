package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.UserGroupEntity;
import com.tateca.tatecabackend.entity.UserGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserGroupRepository extends JpaRepository<UserGroupEntity, UserGroupId> {
    @Query("SELECT uge FROM UserGroupEntity uge WHERE uge.groupUuid = :groupUuid")
    List<UserGroupEntity> findByGroupUuid(UUID groupUuid);

    @Query("SELECT uge FROM UserGroupEntity uge WHERE uge.userUuid IN :userUuidList")
    List<UserGroupEntity> findByUserUuidList(@Param("userUuidList") List<UUID> userUuidList);

}