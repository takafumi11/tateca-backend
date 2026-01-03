package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.CurrencyAccessor;
import com.tateca.tatecabackend.api.client.ExchangeRateApiClient;
import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import com.tateca.tatecabackend.service.impl.InternalExchangeRateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateInternalService Unit Tests")
class InternalExchangeRateServiceUnitTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @Mock
    private CurrencyAccessor currencyAccessor;

    @Mock
    private ExchangeRateApiClient exchangeRateApiClient;

    @InjectMocks
    private InternalExchangeRateServiceImpl exchangeRateInternalService;

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

    // ===== Tests for fetchAndStoreLatestExchangeRate() =====

    // A. Method Orchestration Tests

    @Test
    @DisplayName("Should orchestrate method calls in correct sequence")
    void shouldOrchestrateMethodCallsInCorrectSequence() {
        // Given: API client, accessor, and repository are available
        InOrder inOrder = inOrder(exchangeRateApiClient, currencyAccessor, exchangeRateRepository);

        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of());

        // When: Service fetches and stores exchange rates
        exchangeRateInternalService.fetchAndStoreLatestExchangeRate();

        // Then: Methods should be called in correct order
        inOrder.verify(exchangeRateApiClient, times(1)).fetchLatestExchangeRate();
        inOrder.verify(currencyAccessor, times(1)).findAllById(anyList());
        inOrder.verify(exchangeRateRepository, times(2)).findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class));
        inOrder.verify(exchangeRateRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should call repository batch query twice for two dates")
    void shouldCallRepositoryBatchQueryTwiceForTwoDates() {
        // Given: API returns rates and currencies exist
        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of());

        // When: Service fetches and stores exchange rates
        exchangeRateInternalService.fetchAndStoreLatestExchangeRate();

        // Then: Repository findByCurrencyCodeInAndDate should be called twice (today + tomorrow)
        verify(exchangeRateRepository, times(2)).findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class));
    }

    // B. Currency Filtering Logic Tests

    @Test
    @DisplayName("Should filter out unknown currencies from processing")
    void shouldFilterOutUnknownCurrenciesFromProcessing() {
        // Given: API returns 3 currencies but only 2 exist in master data
        Map<String, Double> ratesWithUnknown = new HashMap<>();
        ratesWithUnknown.put("JPY", 1.0);
        ratesWithUnknown.put("USD", 0.0067);
        ratesWithUnknown.put("UNKNOWN", 999.0);

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
        exchangeRateInternalService.fetchAndStoreLatestExchangeRate();

        // Then: saveAll should only receive entities for known currencies
        verify(exchangeRateRepository, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        assertThat(savedEntities)
                .extracting(ExchangeRateEntity::getCurrencyCode)
                .containsOnly("JPY", "USD")
                .doesNotContain("UNKNOWN");
    }

    @Test
    @DisplayName("Should process all currencies when all are found in master data")
    void shouldProcessAllCurrenciesWhenAllAreFoundInMasterData() {
        // Given: API returns 3 currencies and all exist in master data
        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of());

        // When: Service processes rates
        exchangeRateInternalService.fetchAndStoreLatestExchangeRate();

        // Then: saveAll should receive entities for all currencies
        verify(exchangeRateRepository, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        assertThat(savedEntities)
                .extracting(ExchangeRateEntity::getCurrencyCode)
                .containsOnly("JPY", "USD", "EUR", "JPY", "USD", "EUR");
    }

    // C. Rate Change Detection Logic Tests

    @Test
    @DisplayName("Should skip entity update when rate is unchanged")
    void shouldSkipEntityUpdateWhenRateIsUnchanged() {
        // Given: Existing entity with same rate as API response
        ExchangeRateEntity existingJpy = ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(today)
                .currency(TestFixtures.Currencies.jpy())
                .exchangeRate(BigDecimal.valueOf(1.0))
                .build();

        ExchangeRateEntity spyJpy = spy(existingJpy);

        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(List.of(TestFixtures.Currencies.jpy()));
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of(spyJpy));

        // When: Service processes rates
        exchangeRateInternalService.fetchAndStoreLatestExchangeRate();

        // Then: setExchangeRate should NOT be called (rate unchanged)
        verify(spyJpy, never()).setExchangeRate(any(BigDecimal.class));
    }

    @Test
    @DisplayName("Should update entity when rate has changed")
    void shouldUpdateEntityWhenRateHasChanged() {
        // Given: Existing entity with different rate from API response
        ExchangeRateEntity existingJpy = ExchangeRateEntity.builder()
                .currencyCode("JPY")
                .date(today)
                .currency(TestFixtures.Currencies.jpy())
                .exchangeRate(BigDecimal.valueOf(0.95))
                .build();

        ExchangeRateEntity spyJpy = spy(existingJpy);

        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(List.of(TestFixtures.Currencies.jpy()));
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of(spyJpy));

        // When: Service processes rates
        exchangeRateInternalService.fetchAndStoreLatestExchangeRate();

        // Then: setExchangeRate should be called with new rate
        verify(spyJpy, times(1)).setExchangeRate(BigDecimal.valueOf(1.0));
    }

    @Test
    @DisplayName("Should detect rate change with high precision comparison")
    void shouldDetectRateChangeWithHighPrecisionComparison() {
        // Given: Existing entity with rate 0.006700, API returns 0.006701
        ExchangeRateEntity existingUsd = ExchangeRateEntity.builder()
                .currencyCode("USD")
                .date(today)
                .currency(TestFixtures.Currencies.usd())
                .exchangeRate(new BigDecimal("0.006700"))
                .build();

        ExchangeRateEntity spyUsd = spy(existingUsd);

        Map<String, Double> precisionRates = new HashMap<>();
        precisionRates.put("USD", 0.006701);

        ExchangeRateClientResponse precisionResponse =
                TestFixtures.ExchangeRateApiResponses.withRates(precisionRates);

        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(precisionResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(List.of(TestFixtures.Currencies.usd()));
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of(spyUsd));

        // When: Service processes rates
        exchangeRateInternalService.fetchAndStoreLatestExchangeRate();

        // Then: setExchangeRate should be called (precision difference detected)
        verify(spyUsd, times(1)).setExchangeRate(BigDecimal.valueOf(0.006701));
    }

    // D. Multi-Date Processing Logic Tests

    @Test
    @DisplayName("Should process rates for both today and tomorrow dates")
    void shouldProcessRatesForBothTodayAndTomorrowDates() {
        // Given: API returns rates and no existing records
        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);

        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of());

        // When: Service fetches and stores exchange rates
        exchangeRateInternalService.fetchAndStoreLatestExchangeRate();

        // Then: findByCurrencyCodeInAndDate should be called with both today and tomorrow
        verify(exchangeRateRepository, times(2)).findByCurrencyCodeInAndDate(anyList(), dateCaptor.capture());

        List<LocalDate> capturedDates = dateCaptor.getAllValues();
        assertThat(capturedDates).hasSize(2);
        assertThat(capturedDates.get(0)).isEqualTo(today);
        assertThat(capturedDates.get(1)).isEqualTo(today.plusDays(1));
    }

    @Test
    @DisplayName("Should combine today and tomorrow entities before saving")
    void shouldCombineTodayAndTomorrowEntitiesBeforeSaving() {
        // Given: API returns rates for 3 currencies, no existing records
        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(List.of());

        // When: Service fetches and stores exchange rates
        exchangeRateInternalService.fetchAndStoreLatestExchangeRate();

        // Then: saveAll should be called once with combined list (3 currencies × 2 dates)
        verify(exchangeRateRepository, times(1)).saveAll(entityListCaptor.capture());
        List<ExchangeRateEntity> savedEntities = entityListCaptor.getValue();

        LocalDate tomorrow = today.plusDays(1);
        long todayCount = savedEntities.stream().filter(e -> e.getDate().equals(today)).count();
        long tomorrowCount = savedEntities.stream().filter(e -> e.getDate().equals(tomorrow)).count();

        assertThat(todayCount).isGreaterThan(0);
        assertThat(tomorrowCount).isGreaterThan(0);
    }

    // E. Edge Cases Tests

    @Test
    @DisplayName("Should skip saveAll when no new entities exist")
    void shouldSkipSaveAllWhenNoNewEntitiesExist() {
        // Given: All entities exist with unchanged rates
        List<ExchangeRateEntity> existingEntities = List.of(
                ExchangeRateEntity.builder()
                        .currencyCode("JPY")
                        .date(today)
                        .currency(TestFixtures.Currencies.jpy())
                        .exchangeRate(BigDecimal.valueOf(1.0))
                        .build(),
                ExchangeRateEntity.builder()
                        .currencyCode("USD")
                        .date(today)
                        .currency(TestFixtures.Currencies.usd())
                        .exchangeRate(BigDecimal.valueOf(0.0067))
                        .build(),
                ExchangeRateEntity.builder()
                        .currencyCode("EUR")
                        .date(today)
                        .currency(TestFixtures.Currencies.eur())
                        .exchangeRate(BigDecimal.valueOf(0.0061))
                        .build()
        );

        when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
        when(currencyAccessor.findAllById(anyList())).thenReturn(currencies);
        when(exchangeRateRepository.findByCurrencyCodeInAndDate(anyList(), any(LocalDate.class)))
                .thenReturn(existingEntities);

        // When: Service processes rates
        int result = exchangeRateInternalService.fetchAndStoreLatestExchangeRate();

        // Then: saveAll should NOT be called
        verify(exchangeRateRepository, never()).saveAll(anyList());
        assertThat(result).isZero();
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
