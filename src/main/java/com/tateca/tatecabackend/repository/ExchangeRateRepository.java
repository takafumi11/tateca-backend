package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.entity.ExchangeRateId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRateEntity, ExchangeRateId> {
    Optional<ExchangeRateEntity> findByCurrencyCodeAndDate(String currencyCode, LocalDate date);

    @Query("SELECT e FROM ExchangeRateEntity e WHERE e.date = :date AND e.currencyName.isActive = TRUE")
    List<ExchangeRateEntity> findAllActiveByDate(LocalDate date);
}