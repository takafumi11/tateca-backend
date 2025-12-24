package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.api.client.ExchangeRateApiClient;
import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateUpdateService Unit Tests")
class ExchangeRateUpdateServiceUnitTest {

    @Mock
    private ExchangeRateAccessor exchangeRateAccessor;

    @Mock
    private CurrencyNameAccessor currencyNameAccessor;

    @Mock
    private ExchangeRateApiClient exchangeRateApiClient;

    @InjectMocks
    private ExchangeRateUpdateServiceImpl service;

    @Captor
    private ArgumentCaptor<List<ExchangeRateEntity>> entityListCaptor;

    private ExchangeRateClientResponse apiResponse;
    private List<CurrencyNameEntity> currencies;
    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2024, 1, 15);
        apiResponse = TestFixtures.ExchangeRateApiResponses.success();
        currencies = List.of(
                TestFixtures.Currencies.jpy(),
                TestFixtures.Currencies.usd(),
                TestFixtures.Currencies.eur()
        );
    }

    @Test
    @DisplayName("Should create new exchange rate records for specified date")
    void shouldCreateNewExchangeRateRecordsForSpecifiedDate() {
        // Given: API returns rates for a specified date and currencies exist
        when(exchangeRateApiClient.fetchExchangeRateByDate(testDate)).thenReturn(apiResponse);
        when(currencyNameAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateAccessor.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of()); // Empty list = no existing records

        // When: Service fetches and stores exchange rates
        int result = service.fetchAndStoreExchangeRateByDate(testDate);

        // Then: Should save new records for a specified date only (not next date)
        verify(exchangeRateAccessor, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        // Verify: 3 currencies × 1 date = 3 records
        assertThat(savedEntities).hasSize(3);
        assertThat(result).isEqualTo(3);

        // Verify: All entities have the correct date
        assertThat(savedEntities)
                .allMatch(entity -> entity.getDate().equals(testDate))
                .allMatch(entity -> entity.getCurrencyCode() != null)
                .allMatch(entity -> entity.getExchangeRate() != null);

        // Verify: Contains expected currency codes
        assertThat(savedEntities)
                .extracting(ExchangeRateEntity::getCurrencyCode)
                .containsExactlyInAnyOrder("JPY", "USD", "EUR");
    }

    @Test
    @DisplayName("Should update existing exchange rate records")
    void shouldUpdateExistingExchangeRateRecords() {
        // Given: Existing records in a database
        ExchangeRateEntity existingJpy = ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(testDate)
                .currencyName(TestFixtures.Currencies.jpy())
                .exchangeRate(BigDecimal.valueOf(0.99))  // Old rate
                .build();

        when(exchangeRateApiClient.fetchExchangeRateByDate(testDate)).thenReturn(apiResponse);
        when(currencyNameAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateAccessor.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of(existingJpy)); // Only JPY exists

        // When: Service updates exchange rates
        int result = service.fetchAndStoreExchangeRateByDate(testDate);

        // Then: Should save updated records
        verify(exchangeRateAccessor, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        assertThat(savedEntities).hasSize(3);
        assertThat(result).isEqualTo(3);

        // Verify: JPY rate is updated to a new value
        ExchangeRateEntity updatedJpy = savedEntities.stream()
                .filter(e -> e.getCurrencyCode().equals("JPY"))
                .findFirst()
                .orElseThrow();
        assertThat(updatedJpy.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(1.0));
        // Note: updatedAt is set by @PreUpdate, which is not called in unit tests with mocks
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

        List<CurrencyNameEntity> knownCurrencies = List.of(
                TestFixtures.Currencies.jpy(),
                TestFixtures.Currencies.usd()
        );

        when(exchangeRateApiClient.fetchExchangeRateByDate(testDate)).thenReturn(responseWithUnknown);
        when(currencyNameAccessor.findAllById(anyList())).thenReturn(knownCurrencies);
        when(exchangeRateAccessor.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of()); // No existing records

        // When: Service processes rates
        int result = service.fetchAndStoreExchangeRateByDate(testDate);

        // Then: Should save only known currencies (JPY, USD) = 2 records
        verify(exchangeRateAccessor, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        assertThat(savedEntities).hasSize(2);
        assertThat(result).isEqualTo(2);

        // Verify: UNKNOWN currency is not saved
        assertThat(savedEntities)
                .extracting(ExchangeRateEntity::getCurrencyCode)
                .containsOnly("JPY", "USD")
                .doesNotContain("UNKNOWN");
    }

    @Test
    @DisplayName("Should handle empty conversion rates gracefully")
    void shouldHandleEmptyConversionRates() {
        // Given: API returns empty rates
        Map<String, Double> emptyRates = new HashMap<>();
        ExchangeRateClientResponse emptyResponse =
                TestFixtures.ExchangeRateApiResponses.withRates(emptyRates);

        when(exchangeRateApiClient.fetchExchangeRateByDate(testDate)).thenReturn(emptyResponse);
        when(currencyNameAccessor.findAllById(anyList())).thenReturn(List.of());
        when(exchangeRateAccessor.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of()); // No existing records

        // When: Service processes empty rates
        int result = service.fetchAndStoreExchangeRateByDate(testDate);

        // Then: Should save nothing
        verify(exchangeRateAccessor, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        assertThat(savedEntities).isEmpty();
        assertThat(result).isZero();
    }

    @Test
    @DisplayName("Should skip update when exchange rate is unchanged")
    void shouldSkipUpdateWhenExchangeRateUnchanged() {
        // Given: Existing records with same rates as API response
        ExchangeRateEntity existingJpy = ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(testDate)
                .currencyName(TestFixtures.Currencies.jpy())
                .exchangeRate(BigDecimal.valueOf(1.0))  // Same rate as API
                .build();

        ExchangeRateEntity existingUsd = ExchangeRateEntity.builder()
                .currencyCode("USD")
                .date(testDate)
                .currencyName(TestFixtures.Currencies.usd())
                .exchangeRate(BigDecimal.valueOf(0.0067))  // Same rate as API
                .build();

        when(exchangeRateApiClient.fetchExchangeRateByDate(testDate)).thenReturn(apiResponse);
        when(currencyNameAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateAccessor.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of(existingJpy, existingUsd)); // JPY and USD exist with same rates

        // When: Service processes exchange rates
        int result = service.fetchAndStoreExchangeRateByDate(testDate);

        // Then: Should still save all entities (3 records: 2 unchanged + 1 new EUR)
        verify(exchangeRateAccessor, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        assertThat(savedEntities).hasSize(3);
        assertThat(result).isEqualTo(3);

        // Verify: JPY and USD rates remain unchanged
        ExchangeRateEntity savedJpy = savedEntities.stream()
                .filter(e -> e.getCurrencyCode().equals("JPY"))
                .findFirst()
                .orElseThrow();
        assertThat(savedJpy.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(1.0));

        ExchangeRateEntity savedUsd = savedEntities.stream()
                .filter(e -> e.getCurrencyCode().equals("USD"))
                .findFirst()
                .orElseThrow();
        assertThat(savedUsd.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(0.0067));

        // Verify: EUR is a new record
        ExchangeRateEntity savedEur = savedEntities.stream()
                .filter(e -> e.getCurrencyCode().equals("EUR"))
                .findFirst()
                .orElseThrow();
        assertThat(savedEur.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(0.0061));
    }

    @Test
    @DisplayName("Should call batch query method once")
    void shouldCallBatchQueryOnce() {
        // Given: API returns rates and currencies exist
        when(exchangeRateApiClient.fetchExchangeRateByDate(testDate)).thenReturn(apiResponse);
        when(currencyNameAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateAccessor.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of());

        // When: Service fetches and stores exchange rates
        service.fetchAndStoreExchangeRateByDate(testDate);

        // Then: Batch query method should be called exactly once (N+1 problem solved)
        verify(exchangeRateAccessor, times(1)).findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class));

        // And: saveAll should be called exactly once
        verify(exchangeRateAccessor, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle mixed scenario of new, updated, and unchanged records")
    void shouldHandleMixedScenario() {
        // Given: Mixed scenario
        // - JPY exists with old rate (will be updated)
        // - USD exists with same rate (will be skipped)
        // - EUR does not exist (will be created)
        ExchangeRateEntity existingJpy = ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(testDate)
                .currencyName(TestFixtures.Currencies.jpy())
                .exchangeRate(BigDecimal.valueOf(0.95))  // Old rate, different from API (1.0)
                .build();

        ExchangeRateEntity existingUsd = ExchangeRateEntity.builder()
                .currencyCode("USD")
                .date(testDate)
                .currencyName(TestFixtures.Currencies.usd())
                .exchangeRate(BigDecimal.valueOf(0.0067))  // Same rate as API
                .build();

        when(exchangeRateApiClient.fetchExchangeRateByDate(testDate)).thenReturn(apiResponse);
        when(currencyNameAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateAccessor.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of(existingJpy, existingUsd));

        // When: Service processes exchange rates
        int result = service.fetchAndStoreExchangeRateByDate(testDate);

        // Then: Should save 3 records (1 updated + 1 unchanged + 1 new)
        verify(exchangeRateAccessor, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        assertThat(savedEntities).hasSize(3);
        assertThat(result).isEqualTo(3);

        // Verify: JPY rate is updated
        ExchangeRateEntity savedJpy = savedEntities.stream()
                .filter(e -> e.getCurrencyCode().equals("JPY"))
                .findFirst()
                .orElseThrow();
        assertThat(savedJpy.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(1.0));
        // Note: updatedAt is set by @PreUpdate, which is not called in unit tests with mocks

        // Verify: USD rate is unchanged (but still in the list)
        ExchangeRateEntity savedUsd = savedEntities.stream()
                .filter(e -> e.getCurrencyCode().equals("USD"))
                .findFirst()
                .orElseThrow();
        assertThat(savedUsd.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(0.0067));

        // Verify: EUR is a new record
        ExchangeRateEntity savedEur = savedEntities.stream()
                .filter(e -> e.getCurrencyCode().equals("EUR"))
                .findFirst()
                .orElseThrow();
        assertThat(savedEur.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(0.0061));
        // Note: createdAt is set by @PrePersist, which is not called in unit tests with mocks
    }
}
