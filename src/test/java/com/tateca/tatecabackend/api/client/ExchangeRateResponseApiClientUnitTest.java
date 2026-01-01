package com.tateca.tatecabackend.api.client;

import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateApiClient Unit Tests")
class ExchangeRateResponseApiClientUnitTest {

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
    class FetchLatestExchangeRateResponseTests {

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
}
