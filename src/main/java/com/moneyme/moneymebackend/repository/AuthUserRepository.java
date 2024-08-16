package com.moneyme.moneymebackend.repository;

import com.moneyme.moneymebackend.entity.AuthUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUserEntity, String> {
    boolean existsByEmail(String email);
}
