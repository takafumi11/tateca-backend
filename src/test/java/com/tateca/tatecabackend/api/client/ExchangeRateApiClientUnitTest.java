package com.tateca.tatecabackend.api.client;

import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.service.AbstractServiceUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ExchangeRateApiClient Unit Tests")
class ExchangeRateApiClientUnitTest extends AbstractServiceUnitTest {

    @Mock
    private ExchangeRateHttpClient httpClient;

    private ExchangeRateApiClient apiClient;

    private static final String TEST_API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        apiClient = new ExchangeRateApiClient(httpClient);
        apiClient.setApiKey(TEST_API_KEY);
    }

    @Nested
    @DisplayName("fetchLatestExchangeRate")
    class FetchLatestExchangeRateTests {

        @Test
        @DisplayName("Should call ExchangeRateHttpClient with correct API key")
        void shouldCallHttpClientWithCorrectApiKey() {
            // Given
            ExchangeRateClientResponse expectedResponse = TestFixtures.ExchangeRateApiResponses.success();
            when(httpClient.fetchLatest(TEST_API_KEY)).thenReturn(expectedResponse);

            // When
            apiClient.fetchLatestExchangeRate();

            // Then
            verify(httpClient).fetchLatest(TEST_API_KEY);
        }

        @Test
        @DisplayName("Should return exchange rate response from HTTP client")
        void shouldReturnExchangeRateResponse() {
            // Given
            ExchangeRateClientResponse expectedResponse = TestFixtures.ExchangeRateApiResponses.success();
            when(httpClient.fetchLatest(TEST_API_KEY)).thenReturn(expectedResponse);

            // When
            ExchangeRateClientResponse result = apiClient.fetchLatestExchangeRate();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResult()).isEqualTo("success");
            assertThat(result.getConversionRates()).containsEntry("JPY", 1.0);
        }

        @Test
        @DisplayName("Should throw exception when HTTP client fails")
        void shouldThrowExceptionWhenHttpClientFails() {
            // Given
            when(httpClient.fetchLatest(TEST_API_KEY))
                    .thenThrow(new RestClientException("Connection failed"));

            // When & Then
            assertThatThrownBy(() -> apiClient.fetchLatestExchangeRate())
                    .isInstanceOf(RestClientException.class)
                    .hasMessageContaining("Connection failed");

            verify(httpClient).fetchLatest(TEST_API_KEY);
        }
    }

    @Nested
    @DisplayName("fetchExchangeRateByDate")
    class FetchExchangeRateByDateTests {

        @Test
        @DisplayName("Should call ExchangeRateHttpClient with correct parameters")
        void shouldCallHttpClientWithCorrectParameters() {
            // Given
            LocalDate date = LocalDate.of(2024, 1, 15);
            ExchangeRateClientResponse expectedResponse = TestFixtures.ExchangeRateApiResponses.success();
            when(httpClient.fetchByDate(eq(TEST_API_KEY), anyInt(), anyInt(), anyInt()))
                    .thenReturn(expectedResponse);

            // When
            apiClient.fetchExchangeRateByDate(date);

            // Then
            verify(httpClient).fetchByDate(TEST_API_KEY, 2024, 1, 15);
        }

        @Test
        @DisplayName("Should return exchange rate response for specific date")
        void shouldReturnExchangeRateResponseForDate() {
            // Given
            LocalDate date = LocalDate.of(2024, 1, 15);
            ExchangeRateClientResponse expectedResponse = TestFixtures.ExchangeRateApiResponses.success();
            when(httpClient.fetchByDate(eq(TEST_API_KEY), anyInt(), anyInt(), anyInt()))
                    .thenReturn(expectedResponse);

            // When
            ExchangeRateClientResponse result = apiClient.fetchExchangeRateByDate(date);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResult()).isEqualTo("success");
        }

        @Test
        @DisplayName("Should throw exception when API call fails")
        void shouldThrowExceptionWhenApiCallFails() {
            // Given
            LocalDate date = LocalDate.of(2024, 1, 15);
            when(httpClient.fetchByDate(eq(TEST_API_KEY), anyInt(), anyInt(), anyInt()))
                    .thenThrow(new RestClientException("API error"));

            // When & Then
            assertThatThrownBy(() -> apiClient.fetchExchangeRateByDate(date))
                    .isInstanceOf(RestClientException.class)
                    .hasMessageContaining("API error");

            verify(httpClient).fetchByDate(TEST_API_KEY, 2024, 1, 15);
        }
    }
}
