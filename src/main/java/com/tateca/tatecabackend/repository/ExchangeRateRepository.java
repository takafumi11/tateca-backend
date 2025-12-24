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

    @Query("""
            SELECT e FROM ExchangeRateEntity e
            JOIN FETCH e.currencyName
            WHERE e.date = :date AND e.currencyName.isActive = TRUE
            """)
    List<ExchangeRateEntity> findAllActiveByDate(LocalDate date);

    /**
     * Batch fetch exchange rates for a specific currency across multiple dates.
     * Avoids N+1 queries when loading rates for multiple transaction dates.
     *
     * @param currencyCode Currency code to fetch
     * @param dates List of dates to fetch exchange rates for
     * @return List of exchange rate entities matching the criteria
     */
    @Query("""
            SELECT er FROM ExchangeRateEntity er
            WHERE er.currencyCode = :currencyCode
            AND er.date IN :dates
            """)
    List<ExchangeRateEntity> findByCurrencyCodeAndDates(
        @Param("currencyCode") String currencyCode,
        @Param("dates") List<LocalDate> dates
    );
}