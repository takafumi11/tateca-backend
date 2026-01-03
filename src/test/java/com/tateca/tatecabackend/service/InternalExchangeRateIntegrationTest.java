package com.tateca.tatecabackend.service;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.CurrencyRepository;
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

@DisplayName("ExchangeRate Internal API Integration Tests - Write Operations")
class InternalExchangeRateIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private InternalExchangeRateService service;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    private static final String TEST_API_KEY = "test-exchange-rate-api-key";

    @BeforeEach
    void setUp() {
        WireMock.configureFor(wireMock.getHost(), wireMock.getPort());
        WireMock.reset();

        // Setup currency data in database
        List<CurrencyEntity> currencies = List.of(
                TestFixtures.Currencies.jpy(),
                TestFixtures.Currencies.usd(),
                TestFixtures.Currencies.eur()
        );
        currencyRepository.saveAll(currencies);
        flushAndClear();
    }

    @Nested
    @DisplayName("Given new exchange rate data needs to be stored")
    class WhenNewExchangeRateResponseDataNeedsToBeStored {

        @Test
        @DisplayName("Then should create new records in database")
        void thenShouldCreateNewRecordsInDatabase() {
            // Given: External API returns valid exchange rates
            givenExternalApiReturnsValidLatestRates();

            // When: Updating exchange rates
            int result = service.fetchAndStoreLatestExchangeRate();

            // Then: Should store 6 currency rates in database (3 currencies × 2 dates)
            assertThat(result).isEqualTo(6);

            // And: Database should contain exchange rates for today and tomorrow
            flushAndClear();
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            assertThat(savedRates).hasSize(6);

            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            long todayCount = savedRates.stream()
                    .filter(e -> e.getDate().equals(today))
                    .count();
            long tomorrowCount = savedRates.stream()
                    .filter(e -> e.getDate().equals(tomorrow))
                    .count();
            assertThat(todayCount).isEqualTo(3);
            assertThat(tomorrowCount).isEqualTo(3);

            // And: Rates should match API response for today
            ExchangeRateEntity jpyRateToday = savedRates.stream()
                    .filter(e -> e.getCurrencyCode().equals("JPY"))
                    .filter(e -> e.getDate().equals(today))
                    .findFirst()
                    .orElseThrow();
            assertThat(jpyRateToday.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(1.0));

            ExchangeRateEntity usdRateToday = savedRates.stream()
                    .filter(e -> e.getCurrencyCode().equals("USD"))
                    .filter(e -> e.getDate().equals(today))
                    .findFirst()
                    .orElseThrow();
            assertThat(usdRateToday.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(0.0067));
        }

        @Test
        @DisplayName("Then should handle latest data fetching")
        void thenShouldHandleLatestDataFetching() {
            // Given: External API returns latest exchange rates
            givenExternalApiReturnsValidLatestRates();

            // When: Updating exchange rates
            int result = service.fetchAndStoreLatestExchangeRate();

            // Then: Should successfully store latest data (3 currencies × 2 dates)
            assertThat(result).isEqualTo(6);

            flushAndClear();
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            assertThat(savedRates).hasSize(6);

            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            assertThat(savedRates)
                    .allMatch(e -> e.getDate().equals(today) || e.getDate().equals(tomorrow));
        }

        @Test
        @DisplayName("Then should store rates with today and tomorrow dates")
        void thenShouldStoreRatesWithTodayAndTomorrowDates() {
            // Given: External API returns latest exchange rates
            givenExternalApiReturnsValidLatestRates();

            // When: Updating exchange rates
            int result = service.fetchAndStoreLatestExchangeRate();

            // Then: Should successfully store data with today and tomorrow dates
            assertThat(result).isEqualTo(6);

            flushAndClear();
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            assertThat(savedRates).hasSize(6);

            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);
            long todayCount = savedRates.stream()
                    .filter(e -> e.getDate().equals(today))
                    .count();
            long tomorrowCount = savedRates.stream()
                    .filter(e -> e.getDate().equals(tomorrow))
                    .count();
            assertThat(todayCount).isEqualTo(3);
            assertThat(tomorrowCount).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Given exchange rates already exist in database")
    class WhenExchangeRatesAlreadyExistResponse {

        @Test
        @DisplayName("Then should update existing records when rates change")
        void thenShouldUpdateExistingRecordsWhenRatesChange() {
            // Given: External API returns valid rates
            givenExternalApiReturnsValidLatestRates();

            // And: First update creates records
            service.fetchAndStoreLatestExchangeRate();

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
            givenExternalApiReturnsCustomResponse(responseWithDifferentRates);

            // When: Calling service again with different rates
            service.fetchAndStoreLatestExchangeRate();

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
            givenExternalApiReturnsValidLatestRates();

            // And: First update creates records
            service.fetchAndStoreLatestExchangeRate();

            flushAndClear();
            List<ExchangeRateEntity> firstUpdate = exchangeRateRepository.findAll();
            ExchangeRateEntity jpyBeforeUpdate = firstUpdate.stream()
                    .filter(e -> e.getCurrencyCode().equals("JPY"))
                    .findFirst()
                    .orElseThrow();

            // When: Calling service again with same rates
            service.fetchAndStoreLatestExchangeRate();

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
            givenExternalApiReturnsCustomResponse(responseWithUnknown);

            // When: Updating exchange rates
            int result = service.fetchAndStoreLatestExchangeRate();

            // Then: Should save only known currencies (3 currencies × 2 dates = 6 records)
            assertThat(result).isEqualTo(6);

            flushAndClear();
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            assertThat(savedRates).hasSize(6);
            assertThat(savedRates)
                    .extracting(ExchangeRateEntity::getCurrencyCode)
                    .containsOnly("JPY", "USD", "EUR", "JPY", "USD", "EUR")
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
            givenExternalApiAlwaysFails();

            // When & Then: Should throw exception (after retries handled by API client)
            assertThatThrownBy(() -> service.fetchAndStoreLatestExchangeRate())
                    .hasMessageContaining("Exchange rate service unavailable");

            // And: Database should remain unchanged (transaction rolled back)
            flushAndClear();
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            assertThat(savedRates).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given API returns empty exchange rates")
    class WhenApiReturnsEmptyExchangeRatesResponse {

        @Test
        @DisplayName("Then should complete successfully without saving any data")
        void thenShouldCompleteSuccessfullyWithoutSavingAnyData() {
            // Given: External API returns empty conversion rates
            String emptyResponse = """
                {
                    "result": "success",
                    "time_last_update_unix": "1704067200",
                    "conversion_rates": {}
                }
                """;
            givenExternalApiReturnsCustomResponse(emptyResponse);

            // When: Updating exchange rates
            int result = service.fetchAndStoreLatestExchangeRate();

            // Then: Should return 0 without error
            assertThat(result).isZero();

            // And: Database should remain empty
            flushAndClear();
            List<ExchangeRateEntity> savedRates = exchangeRateRepository.findAll();
            assertThat(savedRates).isEmpty();
        }
    }

    // ========== Helper Methods for Test Setup ==========

    private void givenExternalApiReturnsValidLatestRates() {
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
        givenExternalApiReturnsCustomResponse(responseBody);
    }

    private void givenExternalApiReturnsCustomResponse(String responseBody) {
        String url = String.format("/%s/latest/JPY", TEST_API_KEY);
        stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));
    }

    private void givenExternalApiAlwaysFails() {
        String url = String.format("/%s/latest/JPY", TEST_API_KEY);
        stubFor(get(urlEqualTo(url))
                .willReturn(aResponse().withStatus(500)));
    }
}
