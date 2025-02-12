package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.ExchangeRateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, ExchangeRateId> {
    Optional<ExchangeRateEntity> findByCurrencyCodeAndDate(String currencyCode, LocalDate date);
}