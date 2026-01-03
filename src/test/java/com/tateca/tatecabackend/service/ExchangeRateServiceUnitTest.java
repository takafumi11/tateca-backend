package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.CurrencyAccessor;
import com.tateca.tatecabackend.api.client.ExchangeRateApiClient;
import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import com.tateca.tatecabackend.service.impl.ExchangeRateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService Unit Tests")
class ExchangeRateServiceUnitTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private CurrencyAccessor currencyAccessor;

    @Mock
    private ExchangeRateApiClient exchangeRateApiClient;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    @Captor
    private ArgumentCaptor<List<ExchangeRateEntity>> entityListCaptor;

    private ExchangeRateClientResponse apiResponse;
    private List<CurrencyEntity> currencies;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        apiResponse = TestFixtures.ExchangeRateApiResponses.success();
        currencies = List.of(
                TestFixtures.Currencies.jpy(),
                TestFixtures.Currencies.usd(),
                TestFixtures.Currencies.eur()
        );
    }

    // ===== Tests for getExchangeRate() =====

    @Test
    @DisplayName("Should call repository with correct date and return converted DTO")
    void shouldCallRepositoryWithCorrectDateAndReturnConvertedDTO() {
        // Given: Repository returns exchange rate entities
        LocalDate testDate = LocalDate.now();
        List<ExchangeRateEntity> entities = List.of(
                createExchangeRateEntity(TestFixtures.Currencies.usd(), testDate, new BigDecimal("150.25"))
        );

        when(exchangeRateRepository.findAllActiveByDate(testDate)).thenReturn(entities);

        // When: Getting exchange rate
        ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(testDate);

        // Then: Should call repository with correct date
        verify(exchangeRateRepository, times(1)).findAllActiveByDate(testDate);

        // And: Should return non-null DTO
        assertThat(result).isNotNull();
        assertThat(result.exchangeRateResponseResponseList()).isNotEmpty();
    }

    @Test
    @DisplayName("Should return empty DTO when repository returns empty list")
    void shouldReturnEmptyDTOWhenRepositoryReturnsEmptyList() {
        // Given: Repository returns empty list
        LocalDate testDate = LocalDate.now();

        when(exchangeRateRepository.findAllActiveByDate(testDate)).thenReturn(Collections.emptyList());

        // When: Getting exchange rate
        ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(testDate);

        // Then: Should return DTO with empty list
        assertThat(result).isNotNull();
        assertThat(result.exchangeRateResponseResponseList()).isEmpty();

        verify(exchangeRateRepository, times(1)).findAllActiveByDate(testDate);
    }

    @Test
    @DisplayName("Should propagate DataAccessException from repository layer")
    void shouldPropagateDataAccessExceptionFromRepositoryLayer() {
        // Given: Repository throws DataAccessException
        LocalDate testDate = LocalDate.now();

        when(exchangeRateRepository.findAllActiveByDate(testDate))
                .thenThrow(new DataAccessException("Database connection error") {});

        // When & Then: Should propagate exception without modification
        assertThatThrownBy(() -> exchangeRateService.getExchangeRate(testDate))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Database connection error");

        verify(exchangeRateRepository, times(1)).findAllActiveByDate(testDate);
    }

    // ===== Tests for fetchAndStoreLatestExchangeRate() =====

    @Test
    @DisplayName("Should create new exchange rate records for current date")
    void shouldCreateNewExchangeRateRecordsForCurrentDate() {
        // Given: API returns rates for the current date and currencies exist
        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of()); // Empty list = no existing records

        // When: Service fetches and stores exchange rates
        int result = exchangeRateService.fetchAndStoreLatestExchangeRate();

        // Then: Should save new records for today and tomorrow
        verify(exchangeRateRepository, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        // Verify: 3 currencies × 2 dates = 6 records
        assertThat(savedEntities).hasSize(6);
        assertThat(result).isEqualTo(6);

        // Verify: All entities have valid data
        assertThat(savedEntities)
                .allMatch(entity -> entity.getCurrencyCode() != null)
                .allMatch(entity -> entity.getExchangeRate() != null);

        // Verify: 3 entities for today, 3 for tomorrow
        LocalDate tomorrow = today.plusDays(1);
        long todayCount = savedEntities.stream()
                .filter(e -> e.getDate().equals(today))
                .count();
        long tomorrowCount = savedEntities.stream()
                .filter(e -> e.getDate().equals(tomorrow))
                .count();
        assertThat(todayCount).isEqualTo(3);
        assertThat(tomorrowCount).isEqualTo(3);

        // Verify: Contains expected currency codes (each appears twice)
        assertThat(savedEntities)
                .extracting(ExchangeRateEntity::getCurrencyCode)
                .containsExactlyInAnyOrder("JPY", "USD", "EUR", "JPY", "USD", "EUR");
    }

    @Test
    @DisplayName("Should update existing exchange rate records")
    void shouldUpdateExistingExchangeRateRecords() {
        // Given: Existing records in a database
        ExchangeRateEntity existingJpy = ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(today)
                .currency(TestFixtures.Currencies.jpy())
                .exchangeRate(BigDecimal.valueOf(0.99))  // Old rate
                .build();

        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of(existingJpy)); // Only JPY exists

        // When: Service updates exchange rates
        int result = exchangeRateService.fetchAndStoreLatestExchangeRate();

        // Then: Should save only new records (USD, EUR for today + tomorrow = 4)
        verify(exchangeRateRepository, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        assertThat(savedEntities).hasSize(4);  // USD×2 + EUR×2
        assertThat(result).isEqualTo(4);

        // Verify: Saved entities do not include existing JPY
        assertThat(savedEntities)
                .extracting(ExchangeRateEntity::getCurrencyCode)
                .containsOnly("USD", "EUR")
                .doesNotContain("JPY");
    }

    @Test
    @DisplayName("Should skip currencies not found in CurrencyNameEntity")
    void shouldSkipCurrenciesNotFound() {
        // Given: API returns rates including unknown currency
        Map<String, Double> ratesWithUnknown = new HashMap<>();
        ratesWithUnknown.put("JPY", 1.0);
        ratesWithUnknown.put("USD", 0.0067);
        ratesWithUnknown.put("UNKNOWN", 999.0);  // Unknown currency

        ExchangeRateClientResponse responseWithUnknown =
                TestFixtures.ExchangeRateApiResponses.withRates(ratesWithUnknown);

        List<CurrencyEntity> knownCurrencies = List.of(
                TestFixtures.Currencies.jpy(),
                TestFixtures.Currencies.usd()
        );

        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(responseWithUnknown);
        when(currencyAccessor.findAllById(anyList())).thenReturn(knownCurrencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of());

        // When: Service processes rates
        int result = exchangeRateService.fetchAndStoreLatestExchangeRate();

        // Then: Should save only known currencies (JPY, USD) × 2 dates = 4 records
        verify(exchangeRateRepository, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        assertThat(savedEntities).hasSize(4);
        assertThat(result).isEqualTo(4);

        // Verify: UNKNOWN currency is not saved
        assertThat(savedEntities)
                .extracting(ExchangeRateEntity::getCurrencyCode)
                .containsOnly("JPY", "USD", "JPY", "USD")
                .doesNotContain("UNKNOWN");
    }

    @Test
    @DisplayName("Should handle empty conversion rates gracefully")
    void shouldHandleEmptyConversionRates() {
        // Given: API returns empty rates
        Map<String, Double> emptyRates = new HashMap<>();
        ExchangeRateClientResponse emptyResponse =
                TestFixtures.ExchangeRateApiResponses.withRates(emptyRates);

        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(emptyResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(List.of());
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of());

        // When: Service processes empty rates
        int result = exchangeRateService.fetchAndStoreLatestExchangeRate();

        // Then: Should not call saveAll when there are no entities
        verify(exchangeRateRepository, never()).saveAll(anyList());
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("Should skip update when exchange rate is unchanged")
    void shouldSkipUpdateWhenExchangeRateUnchanged() {
        // Given: Existing records with same rates as API response
        ExchangeRateEntity existingJpy = ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(today)
                .currency(TestFixtures.Currencies.jpy())
                .exchangeRate(BigDecimal.valueOf(1.0))  // Same rate as API
                .build();

        ExchangeRateEntity existingUsd = ExchangeRateEntity.builder()
                .currencyCode("USD")
                .date(today)
                .currency(TestFixtures.Currencies.usd())
                .exchangeRate(BigDecimal.valueOf(0.0067))  // Same rate as API
                .build();

        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of(existingJpy, existingUsd));

        // When: Service processes exchange rates
        int result = exchangeRateService.fetchAndStoreLatestExchangeRate();

        // Then: Should save only new entities (EUR for today + tomorrow = 2)
        verify(exchangeRateRepository, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        assertThat(savedEntities).hasSize(2);  // EUR×2
        assertThat(result).isEqualTo(2);

        // Verify: Saved entities contain only new EUR records
        assertThat(savedEntities)
                .extracting(ExchangeRateEntity::getCurrencyCode)
                .containsOnly("EUR")
                .doesNotContain("JPY", "USD");

        // Verify: EUR for today is in the saved list
        ExchangeRateEntity savedEurToday = savedEntities.stream()
                .filter(e -> e.getDate().equals(today))
                .findFirst()
                .orElseThrow();
        assertThat(savedEurToday.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(0.0061));
        assertThat(savedEurToday.getCurrencyCode()).isEqualTo("EUR");
    }

    @Test
    @DisplayName("Should call batch query method twice (once for today, once for tomorrow)")
    void shouldCallBatchQueryTwice() {
        // Given: API returns rates and currencies exist
        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of());

        // When: Service fetches and stores exchange rates
        exchangeRateService.fetchAndStoreLatestExchangeRate();

        // Then: Batch query method should be called twice (once for today, once for tomorrow)
        verify(exchangeRateRepository, times(2)).findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class));

        // And: saveAll should be called exactly once (with combined entities)
        verify(exchangeRateRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle mixed scenario of new, updated, and unchanged records")
    void shouldHandleMixedScenario() {
        // Given: Mixed scenario
        ExchangeRateEntity existingJpy = ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(today)
                .currency(TestFixtures.Currencies.jpy())
                .exchangeRate(BigDecimal.valueOf(0.95))  // Old rate, different from API (1.0)
                .build();

        ExchangeRateEntity existingUsd = ExchangeRateEntity.builder()
                .currencyCode("USD")
                .date(today)
                .currency(TestFixtures.Currencies.usd())
                .exchangeRate(BigDecimal.valueOf(0.0067))  // Same rate as API
                .build();

        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of(existingJpy, existingUsd));

        // When: Service processes exchange rates
        int result = exchangeRateService.fetchAndStoreLatestExchangeRate();

        // Then: Should save only new entities (EUR for today + tomorrow = 2)
        verify(exchangeRateRepository, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        assertThat(savedEntities).hasSize(2);  // EUR×2
        assertThat(result).isEqualTo(2);

        // Verify: Only new EUR records are in the saved list
        assertThat(savedEntities)
                .extracting(ExchangeRateEntity::getCurrencyCode)
                .containsOnly("EUR")
                .doesNotContain("JPY", "USD");

        // Verify: EUR for today is a new record in the saved list
        ExchangeRateEntity savedEurToday = savedEntities.stream()
                .filter(e -> e.getDate().equals(today))
                .findFirst()
                .orElseThrow();
        assertThat(savedEurToday.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(0.0061));
        assertThat(savedEurToday.getDate()).isEqualTo(today);
    }

    @Test
    @DisplayName("Should store exchange rates for both today and tomorrow")
    void shouldStoreExchangeRatesForBothTodayAndTomorrow() {
        // Given: API returns rates and no existing records
        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of());

        LocalDate tomorrow = today.plusDays(1);

        // When: Service fetches and stores exchange rates
        int result = exchangeRateService.fetchAndStoreLatestExchangeRate();

        // Then: Should save 6 records (3 currencies × 2 dates)
        verify(exchangeRateRepository, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        assertThat(savedEntities).hasSize(6);
        assertThat(result).isEqualTo(6);

        // Verify: 3 entities for today, 3 for tomorrow
        long todayCount = savedEntities.stream()
                .filter(e -> e.getDate().equals(today))
                .count();
        long tomorrowCount = savedEntities.stream()
                .filter(e -> e.getDate().equals(tomorrow))
                .count();

        assertThat(todayCount).isEqualTo(3);
        assertThat(tomorrowCount).isEqualTo(3);

        // Verify: Each currency appears twice (once for each date)
        Map<String, Long> currencyFrequency = savedEntities.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        ExchangeRateEntity::getCurrencyCode,
                        java.util.stream.Collectors.counting()
                ));

        assertThat(currencyFrequency)
                .containsEntry("JPY", 2L)
                .containsEntry("USD", 2L)
                .containsEntry("EUR", 2L);
    }

    private ExchangeRateEntity createExchangeRateEntity(CurrencyEntity currency, LocalDate date, BigDecimal rate) {
        return ExchangeRateEntity.builder()
                .currencyCode(currency.getCurrencyCode())
                .currency(currency)
                .date(date)
                .exchangeRate(rate)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isNew(false)
                .build();
    }
}
