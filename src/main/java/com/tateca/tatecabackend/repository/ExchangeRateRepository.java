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

    /**
     * Find all exchange rates for multiple currency codes on a specific date.
     * This enables batch loading to avoid N+1 queries.
     *
     * @param currencyCodes List of currency codes to fetch
     * @param date Target date
     * @return List of exchange rate entities matching the criteria
     */
    List<ExchangeRateEntity> findByCurrencyCodeInAndDate(List<String> currencyCodes, LocalDate date);

    @Query("SELECT e FROM ExchangeRateEntity e JOIN FETCH e.currencyName WHERE e.date = :date AND e.currencyName.isActive = TRUE")
    List<ExchangeRateEntity> findAllActiveByDate(LocalDate date);
}