package com.tateca.tatecabackend.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.CurrencyNameRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExchangeRateUpdateService Integration Tests")
class ExchangeRateUpdateServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExchangeRateUpdateService service;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyNameRepository currencyNameRepository;

    private static final String TEST_API_KEY = "test-exchange-rate-api-key";

    @BeforeEach
    void setUp() {
        WireMock.configureFor(wireMock.getHost(), wireMock.getPort());
        WireMock.reset();

        // Setup currency data in database
        List<CurrencyNameEntity> currencies = List.of(
                TestFixtures.Currencies.jpy(),
                TestFixtures.Currencies.usd(),
                TestFixtures.Currencies.eur()
        );
        currencyNameRepository.saveAll(currencies);
        flushAndClear();
    }

    @Nested
    @DisplayName("Given new exchange rate data needs to be stored")
    class WhenNewExchangeRateDataNeedsToBeStored {

        @Test
        @DisplayName("Then should create new records in database")
        void thenShouldCreateNewRecordsInDatabase() {
            // Given: External API returns valid exchange rates for a specific date
            LocalDate testDate = LocalDate.of(2024, 1, 15);
            givenExternalApiReturnsValidRatesForDate(testDate);

            // When: Updating exchange rates for that date
            int result = service.fetchAndStoreExchangeRateByDate(testDate);

            // Then: Should store 3 currency rates in database
            assertThat(result).isEqualTo(3);

            // And: Database should contain exchange rates for the specified date
            flushAndClear();
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            assertThat(savedRates).hasSize(3);
            assertThat(savedRates).allMatch(e -> e.getDate().equals(testDate));

            // And: Rates should match API response
            ExchangeRateEntity jpyRate = savedRates.stream()
                    .filter(e -> e.getCurrencyCode().equals("JPY"))
                    .findFirst()
                    .orElseThrow();
            assertThat(jpyRate.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(1.0));

            ExchangeRateEntity usdRate = savedRates.stream()
                    .filter(e -> e.getCurrencyCode().equals("USD"))
                    .findFirst()
                    .orElseThrow();
            assertThat(usdRate.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(0.0067));
        }

        @Test
        @DisplayName("Then should handle past dates for testing purposes")
        void thenShouldHandlePastDatesForTestingPurposes() {
            // Given: External API returns historical data
            LocalDate pastDate = LocalDate.of(2023, 6, 15);
            givenExternalApiReturnsValidRatesForDate(pastDate);

            // When: Updating exchange rates for past date (for testing)
            int result = service.fetchAndStoreExchangeRateByDate(pastDate);

            // Then: Should successfully store historical data
            assertThat(result).isEqualTo(3);

            flushAndClear();
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            assertThat(savedRates)
                    .hasSize(3)
                    .allMatch(e -> e.getDate().equals(pastDate));
        }

        @Test
        @DisplayName("Then should handle future dates for prediction data")
        void thenShouldHandleFutureDatesForPredictionData() {
            // Given: External API returns future prediction data
            LocalDate futureDate = LocalDate.of(2025, 12, 31);
            givenExternalApiReturnsValidRatesForDate(futureDate);

            // When: Updating exchange rates for future date
            int result = service.fetchAndStoreExchangeRateByDate(futureDate);

            // Then: Should successfully store prediction data
            assertThat(result).isEqualTo(3);

            flushAndClear();
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            assertThat(savedRates)
                    .hasSize(3)
                    .allMatch(e -> e.getDate().equals(futureDate));
        }
    }

    @Nested
    @DisplayName("Given exchange rates already exist in database")
    class WhenExchangeRatesAlreadyExist {

        @Test
        @DisplayName("Then should update existing records when rates change")
        void thenShouldUpdateExistingRecordsWhenRatesChange() {
            // Given: External API returns valid rates
            LocalDate testDate = LocalDate.of(2024, 3, 1);
            givenExternalApiReturnsValidRatesForDate(testDate);

            // And: First update creates records
            service.fetchAndStoreExchangeRateByDate(testDate);

            flushAndClear();
            List<ExchangeRateEntity> firstUpdate = exchangeRateRepository.findAll();
            int firstCount = firstUpdate.size();

            // And: API now returns different rates
            String responseWithDifferentRates = """
                {
                    "result": "success",
                    "time_last_update_unix": "1704067200",
                    "conversion_rates": {
                        "JPY": 1.5,
                        "USD": 0.008,
                        "EUR": 0.007
                    }
                }
                """;
            givenExternalApiReturnsCustomResponse(testDate, responseWithDifferentRates);

            // When: Calling service again with same date but different rates
            service.fetchAndStoreExchangeRateByDate(testDate);

            // Then: Should have same number of records (updates, not inserts)
            flushAndClear();
            List<ExchangeRateEntity> secondUpdate = exchangeRateRepository.findAll();
            assertThat(secondUpdate).hasSize(firstCount);

            // And: Rates should be updated to new values
            ExchangeRateEntity jpyAfterUpdate = secondUpdate.stream()
                    .filter(e -> e.getCurrencyCode().equals("JPY"))
                    .findFirst()
                    .orElseThrow();
            assertThat(jpyAfterUpdate.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(1.5));

            ExchangeRateEntity usdAfterUpdate = secondUpdate.stream()
                    .filter(e -> e.getCurrencyCode().equals("USD"))
                    .findFirst()
                    .orElseThrow();
            assertThat(usdAfterUpdate.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(0.008));

            ExchangeRateEntity eurAfterUpdate = secondUpdate.stream()
                    .filter(e -> e.getCurrencyCode().equals("EUR"))
                    .findFirst()
                    .orElseThrow();
            assertThat(eurAfterUpdate.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(0.007));
        }

        @Test
        @DisplayName("Then should not update timestamps when rates are unchanged")
        void thenShouldNotUpdateTimestampsWhenRatesUnchanged() {
            // Given: External API returns valid rates
            LocalDate testDate = LocalDate.of(2024, 3, 15);
            givenExternalApiReturnsValidRatesForDate(testDate);

            // And: First update creates records
            service.fetchAndStoreExchangeRateByDate(testDate);

            flushAndClear();
            List<ExchangeRateEntity> firstUpdate = exchangeRateRepository.findAll();
            ExchangeRateEntity jpyBeforeUpdate = firstUpdate.stream()
                    .filter(e -> e.getCurrencyCode().equals("JPY"))
                    .findFirst()
                    .orElseThrow();

            // When: Calling service again with same date and same rates
            service.fetchAndStoreExchangeRateByDate(testDate);

            // Then: Should have same number of records
            flushAndClear();
            List<ExchangeRateEntity> secondUpdate = exchangeRateRepository.findAll();
            assertThat(secondUpdate).hasSize(firstUpdate.size());

            // And: Rates should remain the same
            ExchangeRateEntity jpyAfterUpdate = secondUpdate.stream()
                    .filter(e -> e.getCurrencyCode().equals("JPY"))
                    .findFirst()
                    .orElseThrow();
            assertThat(jpyAfterUpdate.getExchangeRate())
                    .isEqualByComparingTo(jpyBeforeUpdate.getExchangeRate());

            // And: updatedAt timestamp should NOT be changed (optimization working)
            assertThat(jpyAfterUpdate.getUpdatedAt())
                    .isEqualTo(jpyBeforeUpdate.getUpdatedAt());
        }
    }

    @Nested
    @DisplayName("Given API returns currencies not in master data")
    class WhenApiReturnsCurrenciesNotInMasterData {

        @Test
        @DisplayName("Then should skip unknown currencies and save only known ones")
        void thenShouldSkipUnknownCurrenciesAndSaveOnlyKnownOnes() {
            // Given: API returns rates including currency not in master
            LocalDate testDate = LocalDate.of(2024, 4, 1);
            String responseWithUnknown = """
                {
                    "result": "success",
                    "time_last_update_unix": "1704067200",
                    "conversion_rates": {
                        "JPY": 1.0,
                        "USD": 0.0067,
                        "EUR": 0.0061,
                        "UNKNOWN_CURRENCY": 999.0
                    }
                }
                """;
            givenExternalApiReturnsCustomResponse(testDate, responseWithUnknown);

            // When: Updating exchange rates
            int result = service.fetchAndStoreExchangeRateByDate(testDate);

            // Then: Should save only known currencies (JPY, USD, EUR)
            assertThat(result).isEqualTo(3);

            flushAndClear();
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            assertThat(savedRates)
                    .hasSize(3)
                    .extracting(ExchangeRateEntity::getCurrencyCode)
                    .containsExactlyInAnyOrder("JPY", "USD", "EUR")
                    .doesNotContain("UNKNOWN_CURRENCY");
        }
    }

    @Nested
    @DisplayName("Given external API is unavailable")
    class WhenExternalApiIsUnavailable {

        @Test
        @DisplayName("Then should throw exception and rollback transaction")
        void thenShouldThrowExceptionAndRollbackTransaction() {
            // Given: External API is completely unavailable
            LocalDate testDate = LocalDate.of(2024, 5, 1);
            givenExternalApiAlwaysFailsForDate(testDate);

            // When & Then: Should throw exception (after retries handled by API client)
            assertThatThrownBy(() -> service.fetchAndStoreExchangeRateByDate(testDate))
                    .hasMessageContaining("Exchange rate service unavailable");

            // And: Database should remain unchanged (transaction rolled back)
            flushAndClear();
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            assertThat(savedRates).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given API returns empty exchange rates")
    class WhenApiReturnsEmptyExchangeRates {

        @Test
        @DisplayName("Then should complete successfully without saving any data")
        void thenShouldCompleteSuccessfullyWithoutSavingAnyData() {
            // Given: External API returns empty conversion rates
            LocalDate testDate = LocalDate.of(2024, 6, 1);
            String emptyResponse = """
                {
                    "result": "success",
                    "time_last_update_unix": "1704067200",
                    "conversion_rates": {}
                }
                """;
            givenExternalApiReturnsCustomResponse(testDate, emptyResponse);

            // When: Updating exchange rates
            int result = service.fetchAndStoreExchangeRateByDate(testDate);

            // Then: Should return 0 without error
            assertThat(result).isZero();

            // And: Database should remain empty
            flushAndClear();
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            assertThat(savedRates).isEmpty();
        }
    }

    // ========== Helper Methods for Test Setup ==========

    private void givenExternalApiReturnsValidRatesForDate(LocalDate date) {
        String responseBody = """
            {
                "result": "success",
                "time_last_update_unix": "1704067200",
                "conversion_rates": {
                    "JPY": 1.0,
                    "USD": 0.0067,
                    "EUR": 0.0061
                }
            }
            """;
        givenExternalApiReturnsCustomResponse(date, responseBody);
    }

    private void givenExternalApiReturnsCustomResponse(LocalDate date, String responseBody) {
        String url = String.format("/%s/history/JPY/%d/%d/%d",
                TEST_API_KEY, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    private void givenExternalApiAlwaysFailsForDate(LocalDate date) {
        String url = String.format("/%s/history/JPY/%d/%d/%d",
                TEST_API_KEY, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        stubFor(get(urlEqualTo(url))
                .willReturn(aResponse().withStatus(500)));
    }
}
