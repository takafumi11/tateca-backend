package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CurrencyNameRepository extends JpaRepository<CurrencyNameEntity, String> {
}
