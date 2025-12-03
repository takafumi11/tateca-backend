package com.tateca.tatecabackend.api.client;

import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import com.tateca.tatecabackend.service.AbstractServiceUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ExchangeRateApiClient Unit Tests")
class ExchangeRateApiClientUnitTest extends AbstractServiceUnitTest {

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<String> urlCaptor;

    private ExchangeRateApiClient apiClient;

    private static final String TEST_BASE_URL = "https://api.example.com/v6";
    private static final String TEST_API_KEY = "test-api-key";

    @BeforeEach
    void setUp() {
        apiClient = new ExchangeRateApiClient(restTemplate);
        apiClient.setApiKey(TEST_API_KEY);
        apiClient.setBaseUrl(TEST_BASE_URL);
    }

    @Nested
    @DisplayName("fetchLatestExchangeRate")
    class FetchLatestExchangeRateTests {

        @Test
        @DisplayName("Should build correct URL with baseUrl and apiKey")
        void shouldBuildCorrectUrlWithBaseUrlAndApiKey() {
            // Given
            ExchangeRateClientResponse expectedResponse = createMockResponse();
            when(restTemplate.getForObject(any(String.class), eq(ExchangeRateClientResponse.class)))
                    .thenReturn(expectedResponse);

            // When
            apiClient.fetchLatestExchangeRate();

            // Then
            verify(restTemplate).getForObject(urlCaptor.capture(), eq(ExchangeRateClientResponse.class));
            String capturedUrl = urlCaptor.getValue();
            assertThat(capturedUrl).isEqualTo(TEST_BASE_URL + "/" + TEST_API_KEY + "/latest/JPY");
        }

        @Test
        @DisplayName("Should return exchange rate response from API")
        void shouldReturnExchangeRateResponse() {
            // Given
            ExchangeRateClientResponse expectedResponse = createMockResponse();
            when(restTemplate.getForObject(any(String.class), eq(ExchangeRateClientResponse.class)))
                    .thenReturn(expectedResponse);

            // When
            ExchangeRateClientResponse result = apiClient.fetchLatestExchangeRate();

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getResult()).isEqualTo("success");
            assertThat(result.getConversionRates()).containsEntry("JPY", 1.0);
        }

        @Test
        @DisplayName("Should retry on failure and eventually succeed")
        void shouldRetryOnFailure() {
            // Given
            ExchangeRateClientResponse expectedResponse = createMockResponse();
            when(restTemplate.getForObject(any(String.class), eq(ExchangeRateClientResponse.class)))
                    .thenThrow(new RestClientException("Connection failed"))
                    .thenThrow(new RestClientException("Connection failed"))
                    .thenReturn(expectedResponse);

            // When
            ExchangeRateClientResponse result = apiClient.fetchLatestExchangeRate();

            // Then
            assertThat(result).isNotNull();
            verify(restTemplate, times(3)).getForObject(any(String.class), eq(ExchangeRateClientResponse.class));
        }

        @Test
        @DisplayName("Should throw exception after max retries")
        void shouldThrowExceptionAfterMaxRetries() {
            // Given
            when(restTemplate.getForObject(any(String.class), eq(ExchangeRateClientResponse.class)))
                    .thenThrow(new RestClientException("Connection failed"));

            // When & Then
            assertThatThrownBy(() -> apiClient.fetchLatestExchangeRate())
                    .isInstanceOf(RestClientException.class);

            verify(restTemplate, times(3)).getForObject(any(String.class), eq(ExchangeRateClientResponse.class));
        }
    }

    @Nested
    @DisplayName("fetchExchangeRateByDate")
    class FetchExchangeRateByDateTests {

        @Test
        @DisplayName("Should build correct URL with date parameters")
        void shouldBuildCorrectUrlWithDateParameters() {
            // Given
            LocalDate date = LocalDate.of(2024, 1, 15);
            ExchangeRateClientResponse expectedResponse = createMockResponse();
            when(restTemplate.getForObject(any(String.class), eq(ExchangeRateClientResponse.class)))
                    .thenReturn(expectedResponse);

            // When
            apiClient.fetchExchangeRateByDate(date);

            // Then
            verify(restTemplate).getForObject(urlCaptor.capture(), eq(ExchangeRateClientResponse.class));
            String capturedUrl = urlCaptor.getValue();
            assertThat(capturedUrl).isEqualTo(TEST_BASE_URL + "/" + TEST_API_KEY + "/history/JPY/2024/1/15");
        }

        @Test
        @DisplayName("Should return exchange rate response for specific date")
        void shouldReturnExchangeRateResponseForDate() {
            // Given
            LocalDate date = LocalDate.of(2024, 1, 15);
            ExchangeRateClientResponse expectedResponse = createMockResponse();
            when(restTemplate.getForObject(any(String.class), eq(ExchangeRateClientResponse.class)))
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
            when(restTemplate.getForObject(any(String.class), eq(ExchangeRateClientResponse.class)))
                    .thenThrow(new RestClientException("API error"));

            // When & Then
            assertThatThrownBy(() -> apiClient.fetchExchangeRateByDate(date))
                    .isInstanceOf(RestClientException.class)
                    .hasMessage("API error");
        }
    }

    private ExchangeRateClientResponse createMockResponse() {
        ExchangeRateClientResponse response = new ExchangeRateClientResponse();
        response.setResult("success");
        response.setTimeLastUpdateUnix("1704067200");
        Map<String, Double> rates = new HashMap<>();
        rates.put("JPY", 1.0);
        rates.put("USD", 0.0067);
        response.setConversionRates(rates);
        return response;
    }
}
