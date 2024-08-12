package com.moneyme.moneymebackend.repository;

import com.moneyme.moneymebackend.entity.UserAuthUserEntity;
import com.moneyme.moneymebackend.entity.UserAuthUserId;
import com.moneyme.moneymebackend.entity.UserGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserAuthUserRepository extends JpaRepository<UserAuthUserEntity, UserAuthUserId> {
    @Query("SELECT u FROM UserAuthUserEntity u WHERE u.userUuid = :userUuid")
    List<UserAuthUserEntity> findByUserUuid(UUID userUuid);

    @Query("SELECT u FROM UserAuthUserEntity u WHERE u.authUserUuid = :authUserUuid")
    List<UserAuthUserEntity> findByAuthUserUuid(UUID authUserUuid);
}
