package com.tateca.tatecabackend.repository;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExchangeRateRepository Tests")
class ExchangeRateRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ExchangeRateRepository repository;

    @Autowired
    private CurrencyNameRepository currencyNameRepository;

    private CurrencyNameEntity usd;
    private CurrencyNameEntity eur;
    private CurrencyNameEntity jpy;

    @BeforeEach
    void setUp() {
        // Setup currency names
        usd = TestFixtures.Currencies.usd();
        eur = TestFixtures.Currencies.eur();
        jpy = TestFixtures.Currencies.jpy();

        currencyNameRepository.save(usd);
        currencyNameRepository.save(eur);
        currencyNameRepository.save(jpy);
        flushAndClear();
    }

    @Nested
    @DisplayName("Given multiple exchange rates exist for a currency")
    class WhenMultipleRatesExistForCurrency {

        @Test
        @DisplayName("Then should return the most recent date's rate")
        void thenShouldReturnMostRecentRate() {
            // Given: Multiple USD rates across different dates
            ExchangeRateEntity rate1 = createExchangeRate("USD", LocalDate.of(2024, 1, 1), new BigDecimal("148.00"), usd);
            ExchangeRateEntity rate2 = createExchangeRate("USD", LocalDate.of(2024, 1, 3), new BigDecimal("149.50"), usd);
            ExchangeRateEntity rate3 = createExchangeRate("USD", LocalDate.of(2024, 1, 5), new BigDecimal("150.75"), usd); // Latest

            repository.save(rate1);
            repository.save(rate2);
            repository.save(rate3);
            flushAndClear();

            // When: Finding latest rate for USD
            Optional<ExchangeRateEntity> result = repository.findLatestByCurrencyCode("USD");

            // Then: Should return the rate from 2024-01-05
            assertThat(result).isPresent();
            assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2024, 1, 5));
            assertThat(result.get().getExchangeRate()).isEqualByComparingTo(new BigDecimal("150.75"));
            assertThat(result.get().getCurrencyCode()).isEqualTo("USD");
        }

        @Test
        @DisplayName("Then should return latest even when rates are inserted out of order")
        void thenShouldReturnLatestEvenWhenInsertedOutOfOrder() {
            // Given: Rates inserted in non-chronological order
            ExchangeRateEntity rate1 = createExchangeRate("EUR", LocalDate.of(2024, 1, 10), new BigDecimal("160.00"), eur); // Latest
            ExchangeRateEntity rate2 = createExchangeRate("EUR", LocalDate.of(2024, 1, 5), new BigDecimal("159.00"), eur);
            ExchangeRateEntity rate3 = createExchangeRate("EUR", LocalDate.of(2024, 1, 1), new BigDecimal("158.00"), eur);

            repository.save(rate1);
            repository.save(rate2);
            repository.save(rate3);
            flushAndClear();

            // When: Finding latest rate for EUR
            Optional<ExchangeRateEntity> result = repository.findLatestByCurrencyCode("EUR");

            // Then: Should return the rate with the latest date (2024-01-10)
            assertThat(result).isPresent();
            assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2024, 1, 10));
            assertThat(result.get().getExchangeRate()).isEqualByComparingTo(new BigDecimal("160.00"));
        }
    }

    @Nested
    @DisplayName("Given no exchange rate exists for a currency")
    class WhenNoRateExistsForCurrency {

        @Test
        @DisplayName("Then should return empty Optional")
        void thenShouldReturnEmptyOptional() {
            // Given: No rates exist for GBP (only USD rate exists)
            ExchangeRateEntity usdRate = createExchangeRate("USD", LocalDate.of(2024, 1, 1), new BigDecimal("148.00"), usd);
            repository.save(usdRate);
            flushAndClear();

            // When: Finding latest rate for GBP
            Optional<ExchangeRateEntity> result = repository.findLatestByCurrencyCode("GBP");

            // Then: Should return empty Optional
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Then should return empty Optional when no rates exist at all")
        void thenShouldReturnEmptyOptionalWhenNoRatesExist() {
            // Given: No exchange rates exist in database

            // When: Finding latest rate for any currency
            Optional<ExchangeRateEntity> result = repository.findLatestByCurrencyCode("USD");

            // Then: Should return empty Optional
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given multiple currencies have exchange rates")
    class WhenMultipleCurrenciesHaveRates {

        @Test
        @DisplayName("Then should return correct latest rate for each currency independently")
        void thenShouldReturnCorrectLatestRateForEachCurrency() {
            // Given: USD has rates up to 2024-01-10, EUR has rates up to 2024-01-08
            ExchangeRateEntity usdRate1 = createExchangeRate("USD", LocalDate.of(2024, 1, 5), new BigDecimal("148.00"), usd);
            ExchangeRateEntity usdRate2 = createExchangeRate("USD", LocalDate.of(2024, 1, 10), new BigDecimal("150.00"), usd); // Latest for USD

            ExchangeRateEntity eurRate1 = createExchangeRate("EUR", LocalDate.of(2024, 1, 3), new BigDecimal("158.00"), eur);
            ExchangeRateEntity eurRate2 = createExchangeRate("EUR", LocalDate.of(2024, 1, 8), new BigDecimal("160.00"), eur); // Latest for EUR

            repository.save(usdRate1);
            repository.save(usdRate2);
            repository.save(eurRate1);
            repository.save(eurRate2);
            flushAndClear();

            // When: Finding latest rates for each currency
            Optional<ExchangeRateEntity> usdResult = repository.findLatestByCurrencyCode("USD");
            Optional<ExchangeRateEntity> eurResult = repository.findLatestByCurrencyCode("EUR");

            // Then: Should return different dates for each currency
            assertThat(usdResult).isPresent();
            assertThat(usdResult.get().getDate()).isEqualTo(LocalDate.of(2024, 1, 10));
            assertThat(usdResult.get().getExchangeRate()).isEqualByComparingTo(new BigDecimal("150.00"));

            assertThat(eurResult).isPresent();
            assertThat(eurResult.get().getDate()).isEqualTo(LocalDate.of(2024, 1, 8));
            assertThat(eurResult.get().getExchangeRate()).isEqualByComparingTo(new BigDecimal("160.00"));
        }
    }

    @Nested
    @DisplayName("Given a single exchange rate exists for a currency")
    class WhenSingleRateExistsForCurrency {

        @Test
        @DisplayName("Then should return that single rate")
        void thenShouldReturnThatSingleRate() {
            // Given: Only one USD rate exists
            ExchangeRateEntity singleRate = createExchangeRate("USD", LocalDate.of(2024, 1, 1), new BigDecimal("148.00"), usd);
            repository.save(singleRate);
            flushAndClear();

            // When: Finding latest rate for USD
            Optional<ExchangeRateEntity> result = repository.findLatestByCurrencyCode("USD");

            // Then: Should return that single rate
            assertThat(result).isPresent();
            assertThat(result.get().getDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(result.get().getExchangeRate()).isEqualByComparingTo(new BigDecimal("148.00"));
        }
    }

    @Nested
    @DisplayName("Given exchange rates with future dates exist")
    class WhenFutureDatesExist {

        @Test
        @DisplayName("Then should return the future date if it is the most recent")
        void thenShouldReturnFutureDateIfMostRecent() {
            // Given: Rates exist for past and future dates
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            LocalDate yesterday = today.minusDays(1);

            ExchangeRateEntity pastRate = createExchangeRate("USD", yesterday, new BigDecimal("148.00"), usd);
            ExchangeRateEntity todayRate = createExchangeRate("USD", today, new BigDecimal("149.00"), usd);
            ExchangeRateEntity futureRate = createExchangeRate("USD", tomorrow, new BigDecimal("150.00"), usd); // Latest (future)

            repository.save(pastRate);
            repository.save(todayRate);
            repository.save(futureRate);
            flushAndClear();

            // When: Finding latest rate for USD
            Optional<ExchangeRateEntity> result = repository.findLatestByCurrencyCode("USD");

            // Then: Should return the future date's rate
            assertThat(result).isPresent();
            assertThat(result.get().getDate()).isEqualTo(tomorrow);
            assertThat(result.get().getExchangeRate()).isEqualByComparingTo(new BigDecimal("150.00"));
        }
    }

    // ========== Helper Methods ==========

    private ExchangeRateEntity createExchangeRate(String currencyCode, LocalDate date, BigDecimal rate, CurrencyNameEntity currencyName) {
        return ExchangeRateEntity.builder()
                .currencyCode(currencyCode)
                .date(date)
                .exchangeRate(rate)
                .currencyName(currencyName)
                .build();
    }
}
