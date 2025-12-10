package com.tateca.tatecabackend.scheduler;

import com.tateca.tatecabackend.accessor.CurrencyNameAccessor;
import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.api.client.ExchangeRateApiClient;
import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.service.AbstractServiceUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ExchangeRateScheduler Unit Tests")
class ExchangeRateSchedulerUnitTest extends AbstractServiceUnitTest {

    @Mock
    private ExchangeRateAccessor exchangeRateAccessor;

    @Mock
    private CurrencyNameAccessor currencyNameAccessor;

    @Mock
    private ExchangeRateApiClient exchangeRateApiClient;

    @InjectMocks
    private ExchangeRateScheduler scheduler;

    @Captor
    private ArgumentCaptor<List<ExchangeRateEntity>> entitiesCaptor;

    private ExchangeRateClientResponse apiResponse;
    private LocalDate testDate;
    private LocalDate nextDate;

    @BeforeEach
    void setUp() {
        // Unix timestamp for 2024-01-01 00:00:00 UTC
        testDate = LocalDate.of(2024, 1, 1);
        nextDate = testDate.plusDays(1);

        apiResponse = new ExchangeRateClientResponse();
        apiResponse.setResult("success");
        apiResponse.setTimeLastUpdateUnix("1704067200"); // 2024-01-01 00:00:00 UTC

        Map<String, Double> conversionRates = new HashMap<>();
        conversionRates.put("JPY", 1.0);
        conversionRates.put("USD", 0.0067);
        apiResponse.setConversionRates(conversionRates);
    }

    @Nested
    @DisplayName("fetchAndStoreExchangeRate")
    class FetchAndStoreExchangeRateTests {

        @Test
        @DisplayName("Should fetch from API and save exchange rates for today and tomorrow")
        void shouldFetchAndSaveExchangeRates() {
            // Given
            CurrencyNameEntity jpyCurrency = TestFixtures.Currencies.jpy();
            CurrencyNameEntity usdCurrency = TestFixtures.Currencies.usd();

            when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
            when(currencyNameAccessor.findAllById(anyList()))
                    .thenReturn(List.of(jpyCurrency, usdCurrency));
            when(exchangeRateAccessor.findByCurrencyCodeAndDate(anyString(), any(LocalDate.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

            // When
            scheduler.fetchAndStoreExchangeRate();

            // Then
            verify(exchangeRateApiClient).fetchLatestExchangeRate();
            verify(exchangeRateAccessor).saveAll(entitiesCaptor.capture());

            List<ExchangeRateEntity> savedEntities = entitiesCaptor.getValue();
            // 2 currencies * 2 dates = 4 entities
            assertThat(savedEntities).hasSize(4);
        }

        @Test
        @DisplayName("Should update existing exchange rate when found")
        void shouldUpdateExistingExchangeRate() {
            // Given
            CurrencyNameEntity jpyCurrency = TestFixtures.Currencies.jpy();
            ExchangeRateEntity existingEntity = TestFixtures.ExchangeRates.jpy(testDate);

            Map<String, Double> singleCurrencyRates = new HashMap<>();
            singleCurrencyRates.put("JPY", 1.0);
            apiResponse.setConversionRates(singleCurrencyRates);

            when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
            when(currencyNameAccessor.findAllById(anyList()))
                    .thenReturn(List.of(jpyCurrency));
            when(exchangeRateAccessor.findByCurrencyCodeAndDate(eq("JPY"), eq(testDate)))
                    .thenReturn(existingEntity);
            when(exchangeRateAccessor.findByCurrencyCodeAndDate(eq("JPY"), eq(nextDate)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

            // When
            scheduler.fetchAndStoreExchangeRate();

            // Then
            verify(exchangeRateAccessor).saveAll(entitiesCaptor.capture());
            List<ExchangeRateEntity> savedEntities = entitiesCaptor.getValue();

            assertThat(savedEntities).hasSize(2);
            // First entity is the updated one
            ExchangeRateEntity updatedEntity = savedEntities.stream()
                    .filter(e -> e.getDate().equals(testDate))
                    .findFirst()
                    .orElseThrow();
            assertThat(updatedEntity.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(1.0));
        }

        @Test
        @DisplayName("Should create new exchange rate when not found")
        void shouldCreateNewExchangeRate() {
            // Given
            CurrencyNameEntity jpyCurrency = TestFixtures.Currencies.jpy();

            Map<String, Double> singleCurrencyRates = new HashMap<>();
            singleCurrencyRates.put("JPY", 1.0);
            apiResponse.setConversionRates(singleCurrencyRates);

            when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
            when(currencyNameAccessor.findAllById(anyList()))
                    .thenReturn(List.of(jpyCurrency));
            when(exchangeRateAccessor.findByCurrencyCodeAndDate(anyString(), any(LocalDate.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

            // When
            scheduler.fetchAndStoreExchangeRate();

            // Then
            verify(exchangeRateAccessor).saveAll(entitiesCaptor.capture());
            List<ExchangeRateEntity> savedEntities = entitiesCaptor.getValue();

            assertThat(savedEntities).hasSize(2);
            savedEntities.forEach(entity -> {
                assertThat(entity.getCurrencyCode()).isEqualTo("JPY");
                assertThat(entity.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(1.0));
                assertThat(entity.getCreatedAt()).isNotNull();
            });
        }

        @Test
        @DisplayName("Should skip currency when CurrencyName not found in database")
        void shouldSkipCurrencyWhenCurrencyNameNotFound() {
            // Given
            Map<String, Double> unknownCurrencyRates = new HashMap<>();
            unknownCurrencyRates.put("XXX", 100.0);
            apiResponse.setConversionRates(unknownCurrencyRates);

            when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
            when(currencyNameAccessor.findAllById(anyList())).thenReturn(List.of());

            // When
            scheduler.fetchAndStoreExchangeRate();

            // Then
            verify(exchangeRateAccessor).saveAll(entitiesCaptor.capture());
            List<ExchangeRateEntity> savedEntities = entitiesCaptor.getValue();

            assertThat(savedEntities).isEmpty();
        }

        @Test
        @DisplayName("Should only process currencies that exist in database")
        void shouldOnlyProcessExistingCurrencies() {
            // Given
            CurrencyNameEntity jpyCurrency = TestFixtures.Currencies.jpy();

            Map<String, Double> mixedRates = new HashMap<>();
            mixedRates.put("JPY", 1.0);
            mixedRates.put("XXX", 100.0); // Unknown currency
            apiResponse.setConversionRates(mixedRates);

            when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
            when(currencyNameAccessor.findAllById(anyList()))
                    .thenReturn(List.of(jpyCurrency)); // Only JPY exists
            when(exchangeRateAccessor.findByCurrencyCodeAndDate(anyString(), any(LocalDate.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

            // When
            scheduler.fetchAndStoreExchangeRate();

            // Then
            verify(exchangeRateAccessor).saveAll(entitiesCaptor.capture());
            List<ExchangeRateEntity> savedEntities = entitiesCaptor.getValue();

            // Only JPY should be saved (2 dates)
            assertThat(savedEntities).hasSize(2);
            assertThat(savedEntities).allMatch(e -> e.getCurrencyCode().equals("JPY"));
        }

        @Test
        @DisplayName("Should propagate exception when API fails")
        void shouldPropagateExceptionWhenApiFails() {
            // Given
            when(exchangeRateApiClient.fetchLatestExchangeRate())
                    .thenThrow(new RestClientException("API error"));

            // When & Then
            assertThatThrownBy(() -> scheduler.fetchAndStoreExchangeRate())
                    .isInstanceOf(RestClientException.class)
                    .hasMessage("API error");

            verify(exchangeRateAccessor, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("Should correctly set exchange rate values from API response")
        void shouldCorrectlySetExchangeRateValues() {
            // Given
            CurrencyNameEntity jpyCurrency = TestFixtures.Currencies.jpy();
            CurrencyNameEntity usdCurrency = TestFixtures.Currencies.usd();

            Map<String, Double> rates = new HashMap<>();
            rates.put("JPY", 1.0);
            rates.put("USD", 0.00689655172);
            apiResponse.setConversionRates(rates);

            when(exchangeRateApiClient.fetchLatestExchangeRate()).thenReturn(apiResponse);
            when(currencyNameAccessor.findAllById(anyList()))
                    .thenReturn(List.of(jpyCurrency, usdCurrency));
            when(exchangeRateAccessor.findByCurrencyCodeAndDate(anyString(), any(LocalDate.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found"));

            // When
            scheduler.fetchAndStoreExchangeRate();

            // Then
            verify(exchangeRateAccessor).saveAll(entitiesCaptor.capture());
            List<ExchangeRateEntity> savedEntities = entitiesCaptor.getValue();

            ExchangeRateEntity jpyEntity = savedEntities.stream()
                    .filter(e -> e.getCurrencyCode().equals("JPY") && e.getDate().equals(testDate))
                    .findFirst()
                    .orElseThrow();
            ExchangeRateEntity usdEntity = savedEntities.stream()
                    .filter(e -> e.getCurrencyCode().equals("USD") && e.getDate().equals(testDate))
                    .findFirst()
                    .orElseThrow();

            assertThat(jpyEntity.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(1.0));
            assertThat(usdEntity.getExchangeRate()).isEqualByComparingTo(BigDecimal.valueOf(0.00689655172));
        }
    }
}
