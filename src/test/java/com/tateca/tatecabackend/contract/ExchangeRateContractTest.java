package com.tateca.tatecabackend.contract;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.CurrencyNameRepository;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.*;

/**
 * Contract tests for Exchange Rate API endpoints.
 *
 * <p>These tests verify that the Exchange Rate API responses match the OpenAPI specification.</p>
 */
@DisplayName("Exchange Rate API Contract Tests")
class ExchangeRateContractTest extends AbstractContractTest {

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyNameRepository currencyNameRepository;

    private LocalDate testDate;

    @BeforeEach
    void setUpTestData() {
        testDate = LocalDate.of(2023, 12, 25);

        // Setup currency data in database
        currencyNameRepository.saveAll(List.of(
                TestFixtures.Currencies.usd(),
                TestFixtures.Currencies.eur()
        ));
        flushAndClear();

        // Create exchange rate entities
        ExchangeRateEntity usdRate = ExchangeRateEntity.builder()
                .currencyCode("USD")
                .date(testDate)
                .exchangeRate(new BigDecimal("150.25"))
                .build();

        ExchangeRateEntity eurRate = ExchangeRateEntity.builder()
                .currencyCode("EUR")
                .date(testDate)
                .exchangeRate(new BigDecimal("165.50"))
                .build();

        exchangeRateRepository.saveAll(List.of(usdRate, eurRate));
        flushAndClear();
    }

    @Nested
    @DisplayName("GET /exchange-rate/{date}")
    class GetExchangeRateByDate {

        @Test
        @DisplayName("Should return exchange rates with correct schema")
        void shouldReturnExchangeRatesWithCorrectSchema() {
            given()
                    .contentType("application/json")
            .when()
                    .get("/exchange-rate/{date}", testDate.toString())
            .then()
                    .statusCode(HttpStatus.OK.value())
                    .contentType("application/json")
                    .body("exchange_rate", notNullValue())
                    .body("exchange_rate", isA(java.util.List.class))
                    .body("exchange_rate[0].currency_code", notNullValue())
                    .body("exchange_rate[0].jp_currency_name", notNullValue())
                    .body("exchange_rate[0].eng_currency_name", notNullValue())
                    .body("exchange_rate[0].jp_country_name", notNullValue())
                    .body("exchange_rate[0].eng_country_name", notNullValue())
                    .body("exchange_rate[0].currency_symbol", notNullValue())
                    .body("exchange_rate[0].symbol_position", notNullValue())
                    .body("exchange_rate[0].exchange_rate", notNullValue());
        }

        @Test
        @DisplayName("Should return exchange rates for all currencies in test data")
        void shouldReturnExchangeRatesForAllCurrencies() {
            given()
                    .contentType("application/json")
            .when()
                    .get("/exchange-rate/{date}", testDate.toString())
            .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("exchange_rate.size()", equalTo(2))
                    .body("exchange_rate.currency_code", hasItems("USD", "EUR"))
                    .body("exchange_rate.find { it.currency_code == 'USD' }.exchange_rate", startsWith("150.25"))
                    .body("exchange_rate.find { it.currency_code == 'EUR' }.exchange_rate", startsWith("165.50"));
        }

        @Test
        @DisplayName("Should return 400 for invalid date format")
        void shouldReturn400ForInvalidDateFormat() {
            given()
                    .contentType("application/json")
            .when()
                    .get("/exchange-rate/{date}", "invalid-date")
            .then()
                    .statusCode(HttpStatus.BAD_REQUEST.value());
        }

        @Test
        @DisplayName("Should return empty list for non-existent date")
        void shouldReturnEmptyListForNonExistentDate() {
            given()
                    .contentType("application/json")
            .when()
                    .get("/exchange-rate/{date}", "2099-12-31")
            .then()
                    .statusCode(HttpStatus.OK.value())
                    .body("exchange_rate", notNullValue())
                    .body("exchange_rate.size()", equalTo(0));
        }
    }
}
