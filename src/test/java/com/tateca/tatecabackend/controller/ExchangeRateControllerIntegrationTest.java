package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("ExchangeRateController Integration Tests")
class ExchangeRateControllerIntegrationTest extends AbstractControllerIntegrationTest {

    private CurrencyNameEntity jpyCurrency;
    private CurrencyNameEntity usdCurrency;
    private CurrencyNameEntity eurCurrency;
    private CurrencyNameEntity inactiveCurrency;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2024, 1, 1);

        // Setup currencies
        jpyCurrency = TestFixtures.Currencies.jpy();
        usdCurrency = TestFixtures.Currencies.usd();
        eurCurrency = TestFixtures.Currencies.eur();
        inactiveCurrency = TestFixtures.Currencies.inactive("XXX");

        entityManager.persist(jpyCurrency);
        entityManager.persist(usdCurrency);
        entityManager.persist(eurCurrency);
        entityManager.persist(inactiveCurrency);

        flushAndClear();
    }

    @Nested
    @DisplayName("GET /exchange-rate/{date}")
    class GetExchangeRateTests {

        @Test
        @DisplayName("Should return exchange rates for given date")
        void shouldReturnExchangeRatesForGivenDate() throws Exception {
            // Given
            createAndPersistExchangeRate("JPY", testDate, BigDecimal.ONE, jpyCurrency);
            createAndPersistExchangeRate("USD", testDate, new BigDecimal("0.0067"), usdCurrency);

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "2024-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exchange_rate").isArray())
                    .andExpect(jsonPath("$.exchange_rate.length()").value(2));
        }

        @Test
        @DisplayName("Should return all currency details in response")
        void shouldReturnAllCurrencyDetailsInResponse() throws Exception {
            // Given
            createAndPersistExchangeRate("JPY", testDate, BigDecimal.ONE, jpyCurrency);

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "2024-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exchange_rate[0].currency_code").value("JPY"))
                    .andExpect(jsonPath("$.exchange_rate[0].jp_currency_name").value("日本円"))
                    .andExpect(jsonPath("$.exchange_rate[0].eng_currency_name").value("Japanese Yen"))
                    .andExpect(jsonPath("$.exchange_rate[0].jp_country_name").value("日本"))
                    .andExpect(jsonPath("$.exchange_rate[0].eng_country_name").value("Japan"))
                    .andExpect(jsonPath("$.exchange_rate[0].currency_symbol").value("¥"))
                    .andExpect(jsonPath("$.exchange_rate[0].symbol_position").value("PREFIX"))
                    .andExpect(jsonPath("$.exchange_rate[0].exchange_rate").value("1.000000"));
        }

        @Test
        @DisplayName("Should return only active currencies")
        void shouldReturnOnlyActiveCurrencies() throws Exception {
            // Given
            createAndPersistExchangeRate("JPY", testDate, BigDecimal.ONE, jpyCurrency);
            createAndPersistExchangeRate("XXX", testDate, new BigDecimal("100"), inactiveCurrency);

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "2024-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exchange_rate.length()").value(1))
                    .andExpect(jsonPath("$.exchange_rate[0].currency_code").value("JPY"));
        }

        @Test
        @DisplayName("Should return only rates for specified date")
        void shouldReturnOnlyRatesForSpecifiedDate() throws Exception {
            // Given
            LocalDate differentDate = testDate.plusDays(1);
            createAndPersistExchangeRate("JPY", testDate, BigDecimal.ONE, jpyCurrency);
            createAndPersistExchangeRate("USD", differentDate, new BigDecimal("0.0067"), usdCurrency);

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "2024-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exchange_rate.length()").value(1))
                    .andExpect(jsonPath("$.exchange_rate[0].currency_code").value("JPY"));
        }

        @Test
        @DisplayName("Should return empty list when no rates for date")
        void shouldReturnEmptyListWhenNoRatesForDate() throws Exception {
            // Given - no exchange rates persisted

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "2024-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exchange_rate").isEmpty());
        }

        @Test
        @DisplayName("Should return 400 for invalid date format")
        void shouldReturn400ForInvalidDateFormat() throws Exception {
            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "invalid-date"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 for wrong date pattern")
        void shouldReturn400ForWrongDatePattern() throws Exception {
            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "01-01-2024"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should correctly return exchange rate with decimal precision")
        void shouldCorrectlyReturnExchangeRateWithDecimalPrecision() throws Exception {
            // Given - DB stores DECIMAL(18,6), so use 6 decimal places
            BigDecimal preciseRate = new BigDecimal("0.006897");
            createAndPersistExchangeRate("USD", testDate, preciseRate, usdCurrency);

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "2024-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exchange_rate[0].exchange_rate").value("0.006897"));
        }

        @Test
        @DisplayName("Should return multiple exchange rates for same date")
        void shouldReturnMultipleExchangeRatesForSameDate() throws Exception {
            // Given
            createAndPersistExchangeRate("JPY", testDate, BigDecimal.ONE, jpyCurrency);
            createAndPersistExchangeRate("USD", testDate, new BigDecimal("0.0067"), usdCurrency);
            createAndPersistExchangeRate("EUR", testDate, new BigDecimal("0.0061"), eurCurrency);

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "2024-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exchange_rate.length()").value(3));
        }

        @Test
        @DisplayName("Should handle future date")
        void shouldHandleFutureDate() throws Exception {
            // Given
            LocalDate futureDate = LocalDate.now().plusYears(5);
            createAndPersistExchangeRate("JPY", futureDate, BigDecimal.ONE, jpyCurrency);

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", futureDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exchange_rate.length()").value(1))
                    .andExpect(jsonPath("$.exchange_rate[0].currency_code").value("JPY"));
        }

        @Test
        @DisplayName("Should handle past date")
        void shouldHandlePastDate() throws Exception {
            // Given
            LocalDate pastDate = LocalDate.now().minusYears(5);
            createAndPersistExchangeRate("JPY", pastDate, BigDecimal.ONE, jpyCurrency);

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", pastDate.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exchange_rate.length()").value(1))
                    .andExpect(jsonPath("$.exchange_rate[0].currency_code").value("JPY"));
        }
    }

    private void createAndPersistExchangeRate(String currencyCode, LocalDate date,
                                               BigDecimal rate, CurrencyNameEntity currency) {
        ExchangeRateEntity entity = ExchangeRateEntity.builder()
                .currencyCode(currencyCode)
                .date(date)
                .currencyName(currency)
                .exchangeRate(rate)
                .build();
        entityManager.persist(entity);
        flushAndClear();
    }
}
