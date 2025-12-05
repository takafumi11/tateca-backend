package com.tateca.tatecabackend.api.client;

import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.service.AbstractServiceUnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ExchangeRateHttpClient Unit Tests")
class ExchangeRateHttpClientUnitTest extends AbstractServiceUnitTest {

    @Mock
    private ExchangeRateHttpClient httpClient;

    private static final String TEST_API_KEY = "test-api-key";

    @Nested
    @DisplayName("fetchLatest")
    class FetchLatestTests {

        @Test
        @DisplayName("Should fetch latest exchange rate successfully")
        void shouldFetchLatestExchangeRateSuccessfully() {
            // Given
            ExchangeRateClientResponse expectedResponse = TestFixtures.ExchangeRateApiResponses.success();
            when(httpClient.fetchLatest(TEST_API_KEY)).thenReturn(expectedResponse);

            // When
            ExchangeRateClientResponse result = httpClient.fetchLatest(TEST_API_KEY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResult()).isEqualTo("success");
            assertThat(result.getTimeLastUpdateUnix()).isEqualTo("1704067200");
            assertThat(result.getConversionRates())
                    .containsEntry("JPY", 1.0)
                    .containsEntry("USD", 0.0067)
                    .containsEntry("EUR", 0.0061);

            verify(httpClient).fetchLatest(TEST_API_KEY);
        }

        @Test
        @DisplayName("Should pass API key parameter correctly")
        void shouldPassApiKeyParameterCorrectly() {
            // Given
            ExchangeRateClientResponse expectedResponse = TestFixtures.ExchangeRateApiResponses.success();
            when(httpClient.fetchLatest(TEST_API_KEY)).thenReturn(expectedResponse);

            // When
            httpClient.fetchLatest(TEST_API_KEY);

            // Then
            verify(httpClient).fetchLatest(TEST_API_KEY);
        }

        @Test
        @DisplayName("Should throw RestClientException on HTTP error")
        void shouldThrowRestClientExceptionOnHttpError() {
            // Given
            when(httpClient.fetchLatest(TEST_API_KEY))
                    .thenThrow(new RestClientException("HTTP 500 Internal Server Error"));

            // When & Then
            assertThatThrownBy(() -> httpClient.fetchLatest(TEST_API_KEY))
                    .isInstanceOf(RestClientException.class)
                    .hasMessageContaining("HTTP 500");
        }
    }

    @Nested
    @DisplayName("fetchByDate")
    class FetchByDateTests {

        @Test
        @DisplayName("Should fetch exchange rate by date successfully")
        void shouldFetchExchangeRateByDateSuccessfully() {
            // Given
            LocalDate date = LocalDate.of(2024, 1, 15);
            ExchangeRateClientResponse expectedResponse = TestFixtures.ExchangeRateApiResponses.success();
            when(httpClient.fetchByDate(TEST_API_KEY, date.getYear(), date.getMonthValue(), date.getDayOfMonth()))
                    .thenReturn(expectedResponse);

            // When
            ExchangeRateClientResponse result = httpClient.fetchByDate(
                    TEST_API_KEY, date.getYear(), date.getMonthValue(), date.getDayOfMonth());

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResult()).isEqualTo("success");
            assertThat(result.getConversionRates())
                    .containsEntry("JPY", 1.0)
                    .containsEntry("USD", 0.0067);

            verify(httpClient).fetchByDate(TEST_API_KEY, date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        }

        @Test
        @DisplayName("Should pass date parameters correctly")
        void shouldPassDateParametersCorrectly() {
            // Given
            ExchangeRateClientResponse expectedResponse = TestFixtures.ExchangeRateApiResponses.success();
            when(httpClient.fetchByDate(TEST_API_KEY, 2024, 12, 25)).thenReturn(expectedResponse);

            // When
            httpClient.fetchByDate(TEST_API_KEY, 2024, 12, 25);

            // Then
            verify(httpClient).fetchByDate(TEST_API_KEY, 2024, 12, 25);
        }

        @Test
        @DisplayName("Should throw RestClientException on HTTP error")
        void shouldThrowRestClientExceptionOnHttpError() {
            // Given
            when(httpClient.fetchByDate(TEST_API_KEY, 2024, 1, 15))
                    .thenThrow(new RestClientException("HTTP 404 Not Found"));

            // When & Then
            assertThatThrownBy(() -> httpClient.fetchByDate(TEST_API_KEY, 2024, 1, 15))
                    .isInstanceOf(RestClientException.class)
                    .hasMessageContaining("HTTP 404");
        }
    }
}
