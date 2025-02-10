package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.AuthUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuthUserRepository extends JpaRepository<AuthUserEntity, String> {
    boolean existsByEmail(String email);
}
