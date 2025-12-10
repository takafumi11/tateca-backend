package com.tateca.tatecabackend.scheduler;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.tateca.tatecabackend.controller.AbstractControllerIntegrationTest;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExchangeRateScheduler Integration Tests")
class ExchangeRateSchedulerIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired
    private ExchangeRateScheduler scheduler;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    private CurrencyNameEntity jpyCurrency;
    private CurrencyNameEntity usdCurrency;

    private static final String TEST_API_KEY = "test-exchange-rate-api-key";

    @BeforeEach
    void setUp() {
        WireMock.configureFor(wireMock.getHost(), wireMock.getPort());
        WireMock.reset();

        // Setup currencies
        jpyCurrency = TestFixtures.Currencies.jpy();
        usdCurrency = TestFixtures.Currencies.usd();

        entityManager.persist(jpyCurrency);
        entityManager.persist(usdCurrency);
        entityManager.flush();
        entityManager.clear();
    }

    @Nested
    @DisplayName("fetchAndStoreExchangeRate")
    class FetchAndStoreExchangeRateTests {

        @Test
        @DisplayName("Should fetch from API and save new exchange rates to database")
        void shouldFetchAndSaveNewExchangeRates() {
            // Given
            String responseBody = """
                {
                    "result": "success",
                    "time_last_update_unix": "1704067200",
                    "conversion_rates": {
                        "JPY": 1.0,
                        "USD": 0.0067
                    }
                }
                """;

            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/latest/JPY"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(responseBody)));

            // When
            scheduler.fetchAndStoreExchangeRate();
            entityManager.flush();
            entityManager.clear();

            // Then
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            // 2 currencies * 2 dates (today and tomorrow) = 4 entries
            assertThat(savedRates).hasSize(4);

            // Verify JPY rate exists
            assertThat(savedRates)
                    .filteredOn(rate -> rate.getCurrencyCode().equals("JPY"))
                    .hasSize(2)
                    .allSatisfy(rate -> {
                        assertThat(rate.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(1.0));
                    });

            // Verify USD rate exists
            assertThat(savedRates)
                    .filteredOn(rate -> rate.getCurrencyCode().equals("USD"))
                    .hasSize(2)
                    .allSatisfy(rate -> {
                        assertThat(rate.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(0.0067));
                    });
        }

        @Test
        @DisplayName("Should update existing exchange rates in database")
        void shouldUpdateExistingExchangeRates() {
            // Given - Create existing rate
            LocalDate date = LocalDate.of(2024, 1, 1);
            ExchangeRateEntity existingRate = ExchangeRateEntity.builder()
                    .currencyCode("JPY")
                    .date(date)
                    .currencyName(jpyCurrency)
                    .exchangeRate(BigDecimal.valueOf(0.5))
                    .build();
            entityManager.persist(existingRate);
            entityManager.flush();
            entityManager.clear();

            String responseBody = """
                {
                    "result": "success",
                    "time_last_update_unix": "1704067200",
                    "conversion_rates": {
                        "JPY": 1.0
                    }
                }
                """;

            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/latest/JPY"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(responseBody)));

            // When
            scheduler.fetchAndStoreExchangeRate();
            entityManager.flush();
            entityManager.clear();

            // Then
            List<ExchangeRateEntity> rates = exchangeRateRepository.findAll();
            ExchangeRateEntity updatedRate = rates.stream()
                    .filter(r -> r.getCurrencyCode().equals("JPY") && r.getDate().equals(date))
                    .findFirst()
                    .orElseThrow();

            assertThat(updatedRate.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(1.0));
        }

        @Test
        @DisplayName("Should skip currencies not found in database")
        void shouldSkipUnknownCurrencies() {
            // Given - Only JPY and USD exist in DB, but API returns EUR too
            String responseBody = """
                {
                    "result": "success",
                    "time_last_update_unix": "1704067200",
                    "conversion_rates": {
                        "JPY": 1.0,
                        "USD": 0.0067,
                        "EUR": 0.0061,
                        "GBP": 0.0052
                    }
                }
                """;

            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/latest/JPY"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(responseBody)));

            // When
            scheduler.fetchAndStoreExchangeRate();
            entityManager.flush();
            entityManager.clear();

            // Then - Only JPY and USD should be saved
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();

            assertThat(savedRates)
                    .extracting(ExchangeRateEntity::getCurrencyCode)
                    .containsOnly("JPY", "USD");

            // EUR and GBP should not be saved
            assertThat(savedRates)
                    .extracting(ExchangeRateEntity::getCurrencyCode)
                    .doesNotContain("EUR", "GBP");
        }

        @Test
        @DisplayName("Should persist correct exchange rate values with precision")
        void shouldPersistCorrectExchangeRateValues() {
            // Given
            String responseBody = """
                {
                    "result": "success",
                    "time_last_update_unix": "1704067200",
                    "conversion_rates": {
                        "USD": 0.006897
                    }
                }
                """;

            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/latest/JPY"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(responseBody)));

            // When
            scheduler.fetchAndStoreExchangeRate();
            entityManager.flush();
            entityManager.clear();

            // Then
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            ExchangeRateEntity usdRate = savedRates.stream()
                    .filter(r -> r.getCurrencyCode().equals("USD"))
                    .findFirst()
                    .orElseThrow();

            assertThat(usdRate.getExchangeRate()).isEqualByComparingTo(new BigDecimal("0.006897"));
        }
    }
}
