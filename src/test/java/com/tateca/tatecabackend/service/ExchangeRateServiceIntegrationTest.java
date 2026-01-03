package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;
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

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ExchangeRateQueryService Integration Tests")
class ExchangeRateServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExchangeRateService exchangeRateService;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private CurrencyRepository currencyRepository;

    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now();

        // Create currencies
        CurrencyEntity usdCurrency = TestFixtures.Currencies.usd();
        CurrencyEntity eurCurrency = TestFixtures.Currencies.eur();
        CurrencyEntity gbpCurrency = createGbpCurrency();

        currencyRepository.save(usdCurrency);
        currencyRepository.save(eurCurrency);
        currencyRepository.save(gbpCurrency);

        // Create exchange rates for test date
        ExchangeRateEntity usdRate = ExchangeRateEntity.builder()
                .currencyCode("USD")
                .date(testDate)
                .exchangeRate(new BigDecimal("150.25"))
                .isNew(true)
                .build();

        ExchangeRateEntity eurRate = ExchangeRateEntity.builder()
                .currencyCode("EUR")
                .date(testDate)
                .exchangeRate(new BigDecimal("165.50"))
                .isNew(true)
                .build();

        ExchangeRateEntity gbpRate = ExchangeRateEntity.builder()
                .currencyCode("GBP")
                .date(testDate)
                .exchangeRate(new BigDecimal("190.75"))
                .isNew(true)
                .build();

        exchangeRateRepository.save(usdRate);
        exchangeRateRepository.save(eurRate);
        exchangeRateRepository.save(gbpRate);

        flushAndClear();
    }

    @Nested
    @DisplayName("Given exchange rates exist in database for specific date")
    class WhenExchangeRatesExistForDate {

        @Test
        @DisplayName("Then should retrieve all exchange rates for that date")
        void thenShouldRetrieveAllExchangeRatesForDate() {
            // When: Getting exchange rate for test date
            ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(testDate);

            // Then: Should return all exchange rates
            assertThat(result).isNotNull();
            assertThat(result.exchangeRateResponseResponseList()).hasSize(3);

            // And: Should contain USD rate
            assertThat(result.exchangeRateResponseResponseList())
                    .anySatisfy(rate -> {
                        assertThat(rate.currencyCode()).isEqualTo("USD");
                        assertThat(new BigDecimal(rate.exchangeRate())).isEqualByComparingTo(new BigDecimal("150.25"));
                        assertThat(rate.jpCurrencyName()).isEqualTo("米ドル");
                        assertThat(rate.engCurrencyName()).isEqualTo("US Dollar");
                    });

            // And: Should contain EUR rate
            assertThat(result.exchangeRateResponseResponseList())
                    .anySatisfy(rate -> {
                        assertThat(rate.currencyCode()).isEqualTo("EUR");
                        assertThat(new BigDecimal(rate.exchangeRate())).isEqualByComparingTo(new BigDecimal("165.50"));
                        assertThat(rate.jpCurrencyName()).isEqualTo("ユーロ");
                        assertThat(rate.engCurrencyName()).isEqualTo("Euro");
                    });

            // And: Should contain GBP rate
            assertThat(result.exchangeRateResponseResponseList())
                    .anySatisfy(rate -> {
                        assertThat(rate.currencyCode()).isEqualTo("GBP");
                        assertThat(new BigDecimal(rate.exchangeRate())).isEqualByComparingTo(new BigDecimal("190.75"));
                        assertThat(rate.jpCurrencyName()).isEqualTo("英ポンド");
                        assertThat(rate.engCurrencyName()).isEqualTo("British Pound");
                    });
        }

        @Test
        @DisplayName("Then should include currency metadata from joined Currency table")
        void thenShouldIncludeCurrencyMetadataFromJoinedTable() {
            // When: Getting exchange rate
            ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(testDate);

            // Then: Should include full currency metadata
            assertThat(result.exchangeRateResponseResponseList())
                    .allSatisfy(rate -> {
                        assertThat(rate.currencyCode()).isNotNull();
                        assertThat(rate.jpCurrencyName()).isNotNull();
                        assertThat(rate.engCurrencyName()).isNotNull();
                        assertThat(rate.jpCountryName()).isNotNull();
                        assertThat(rate.engCountryName()).isNotNull();
                        assertThat(rate.currencySymbol()).isNotNull();
                        assertThat(rate.symbolPosition()).isNotNull();
                        assertThat(rate.exchangeRate()).isNotNull();
                    });
        }

        @Test
        @DisplayName("Then should preserve decimal precision from database")
        void thenShouldPreserveDecimalPrecisionFromDatabase() {
            // Given: Exchange rate with high precision
            ExchangeRateEntity preciseRate = ExchangeRateEntity.builder()
                    .currencyCode("USD")
                    .date(testDate.plusDays(1))
                    .exchangeRate(new BigDecimal("150.123456"))
                    .isNew(true)
                    .build();
            exchangeRateRepository.save(preciseRate);
            flushAndClear();

            // When: Getting exchange rate
            ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(testDate.plusDays(1));

            // Then: Should preserve decimal precision
            assertThat(result.exchangeRateResponseResponseList())
                    .anySatisfy(rate -> {
                        assertThat(rate.currencyCode()).isEqualTo("USD");
                        assertThat(new BigDecimal(rate.exchangeRate())).isEqualByComparingTo(new BigDecimal("150.123456"));
                    });
        }
    }

    @Nested
    @DisplayName("Given no exchange rates exist for specific date")
    class WhenNoExchangeRatesExistForDate {

        @Test
        @DisplayName("Then should return empty list")
        void thenShouldReturnEmptyList() {
            // Given: Date with no exchange rates (10 years from now)
            LocalDate emptyDate = LocalDate.now().plusYears(10);

            // When: Getting exchange rate
            ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(emptyDate);

            // Then: Should return empty list
            assertThat(result).isNotNull();
            assertThat(result.exchangeRateResponseResponseList()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Given exchange rates exist for multiple dates")
    class WhenExchangeRatesExistForMultipleDates {

        @Test
        @DisplayName("Then should return only rates for requested date")
        void thenShouldReturnOnlyRatesForRequestedDate() {
            // Given: Exchange rates for different dates
            LocalDate anotherDate = testDate.plusDays(1);

            ExchangeRateEntity usdRateAnotherDate = ExchangeRateEntity.builder()
                    .currencyCode("USD")
                    .date(anotherDate)
                    .exchangeRate(new BigDecimal("151.00"))
                    .isNew(true)
                    .build();

            exchangeRateRepository.save(usdRateAnotherDate);
            flushAndClear();

            // When: Getting exchange rate for original test date
            ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(testDate);

            // Then: Should return only rates for requested date
            assertThat(result.exchangeRateResponseResponseList()).hasSize(3);
            assertThat(result.exchangeRateResponseResponseList())
                    .anySatisfy(rate -> {
                        assertThat(rate.currencyCode()).isEqualTo("USD");
                        assertThat(new BigDecimal(rate.exchangeRate())).isEqualByComparingTo(new BigDecimal("150.25")); // Original rate, not 151.00
                    });
        }
    }

    @Nested
    @DisplayName("Given exchange rates exist for only active currencies")
    class WhenExchangeRatesExistForActiveCurrencies {

        @Test
        @DisplayName("Then should return only rates for active currencies")
        void thenShouldReturnOnlyRatesForActiveCurrencies() {
            // Given: Inactive currency with exchange rate
            CurrencyEntity inactiveCurrency = CurrencyEntity.builder()
                    .currencyCode("XXX")
                    .jpCurrencyName("無効通貨")
                    .engCurrencyName("Inactive Currency")
                    .jpCountryName("なし")
                    .engCountryName("None")
                    .isActive(false)
                    .currencySymbol("X")
                    .symbolPosition(com.tateca.tatecabackend.model.SymbolPosition.PREFIX)
                    .build();
            currencyRepository.save(inactiveCurrency);

            ExchangeRateEntity inactiveRate = ExchangeRateEntity.builder()
                    .currencyCode("XXX")
                    .date(testDate)
                    .exchangeRate(new BigDecimal("999.99"))
                    .isNew(true)
                    .build();
            exchangeRateRepository.save(inactiveRate);
            flushAndClear();

            // When: Getting exchange rate
            ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(testDate);

            // Then: Should not include inactive currency
            assertThat(result.exchangeRateResponseResponseList())
                    .hasSize(3)
                    .noneMatch(rate -> rate.currencyCode().equals("XXX"));
        }
    }

    @Nested
    @DisplayName("Given exchange rates for past and future dates")
    class WhenExchangeRatesExistForPastAndFutureDates {

        @Test
        @DisplayName("Then should support past dates")
        void thenShouldSupportPastDates() {
            // Given: Past date (5 years ago)
            LocalDate pastDate = LocalDate.now().minusYears(5);
            ExchangeRateEntity pastRate = ExchangeRateEntity.builder()
                    .currencyCode("USD")
                    .date(pastDate)
                    .exchangeRate(new BigDecimal("108.50"))
                    .isNew(true)
                    .build();
            exchangeRateRepository.save(pastRate);
            flushAndClear();

            // When: Getting exchange rate for past date
            ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(pastDate);

            // Then: Should return past rate
            assertThat(result.exchangeRateResponseResponseList()).hasSize(1);
            assertThat(new BigDecimal(result.exchangeRateResponseResponseList().get(0).exchangeRate())).isEqualByComparingTo(new BigDecimal("108.50"));
        }

        @Test
        @DisplayName("Then should support future dates")
        void thenShouldSupportFutureDates() {
            // Given: Future date (5 years from now)
            LocalDate futureDate = LocalDate.now().plusYears(5);
            ExchangeRateEntity futureRate = ExchangeRateEntity.builder()
                    .currencyCode("EUR")
                    .date(futureDate)
                    .exchangeRate(new BigDecimal("180.00"))
                    .isNew(true)
                    .build();
            exchangeRateRepository.save(futureRate);
            flushAndClear();

            // When: Getting exchange rate for future date
            ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(futureDate);

            // Then: Should return future rate
            assertThat(result.exchangeRateResponseResponseList()).hasSize(1);
            assertThat(new BigDecimal(result.exchangeRateResponseResponseList().get(0).exchangeRate())).isEqualByComparingTo(new BigDecimal("180.00"));
        }
    }

    private CurrencyEntity createGbpCurrency() {
        return CurrencyEntity.builder()
                .currencyCode("GBP")
                .jpCurrencyName("英ポンド")
                .engCurrencyName("British Pound")
                .jpCountryName("イギリス")
                .engCountryName("United Kingdom")
                .isActive(true)
                .currencySymbol("£")
                .symbolPosition(com.tateca.tatecabackend.model.SymbolPosition.PREFIX)
                .build();
    }
}
