package com.moneyme.moneymebackend.repository;

import com.moneyme.moneymebackend.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {
    @Query("SELECT u from UserEntity u WHERE u.uid = :uid")
    List<UserEntity> findByUid(String uid);
}
