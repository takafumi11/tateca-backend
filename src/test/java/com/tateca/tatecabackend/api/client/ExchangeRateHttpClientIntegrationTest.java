package com.tateca.tatecabackend.api.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClientException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExchangeRateHttpClient Integration Tests")
class ExchangeRateHttpClientIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExchangeRateHttpClient httpClient;

    private static final String TEST_API_KEY = "test-exchange-rate-api-key";

    @BeforeEach
    void setUp() {
        WireMock.configureFor(wireMock.getHost(), wireMock.getPort());
        WireMock.reset();
    }

    @Nested
    @DisplayName("fetchLatest")
    class FetchLatestTests {

        @Test
        @DisplayName("Should fetch latest exchange rate from API")
        void shouldFetchLatestExchangeRateFromApi() {
            // Given
            String responseBody = """
                {
                    "result": "success",
                    "time_last_update_unix": "1704067200",
                    "conversion_rates": {
                        "JPY": 1.0,
                        "USD": 0.0067,
                        "EUR": 0.0061
                    }
                }
                """;

            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/latest/JPY"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(responseBody)));

            // When
            ExchangeRateClientResponse response = httpClient.fetchLatest(TEST_API_KEY);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getResult()).isEqualTo("success");
            assertThat(response.getTimeLastUpdateUnix()).isEqualTo("1704067200");
            assertThat(response.getConversionRates())
                    .containsEntry("JPY", 1.0)
                    .containsEntry("USD", 0.0067)
                    .containsEntry("EUR", 0.0061);

            verify(getRequestedFor(urlEqualTo("/" + TEST_API_KEY + "/latest/JPY")));
        }

        @Test
        @DisplayName("Should throw RestClientException on server error")
        void shouldThrowRestClientExceptionOnServerError() {
            // Given
            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/latest/JPY"))
                    .willReturn(aResponse().withStatus(500)));

            // When & Then
            assertThatThrownBy(() -> httpClient.fetchLatest(TEST_API_KEY))
                    .isInstanceOf(RestClientException.class);
        }

        @Test
        @DisplayName("Should throw RestClientException on not found error")
        void shouldThrowRestClientExceptionOnNotFoundError() {
            // Given
            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/latest/JPY"))
                    .willReturn(aResponse().withStatus(404)));

            // When & Then
            assertThatThrownBy(() -> httpClient.fetchLatest(TEST_API_KEY))
                    .isInstanceOf(RestClientException.class);
        }
    }

    @Nested
    @DisplayName("fetchByDate")
    class FetchByDateTests {

        @Test
        @DisplayName("Should fetch exchange rate for specific date from API")
        void shouldFetchExchangeRateByDateFromApi() {
            // Given
            String responseBody = """
                {
                    "result": "success",
                    "time_last_update_unix": "1705276800",
                    "conversion_rates": {
                        "JPY": 1.0,
                        "USD": 0.0068
                    }
                }
                """;

            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/history/JPY/2024/1/15"))
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(responseBody)));

            // When
            ExchangeRateClientResponse response = httpClient.fetchByDate(TEST_API_KEY, 2024, 1, 15);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getResult()).isEqualTo("success");
            assertThat(response.getConversionRates())
                    .containsEntry("JPY", 1.0)
                    .containsEntry("USD", 0.0068);

            verify(getRequestedFor(urlEqualTo("/" + TEST_API_KEY + "/history/JPY/2024/1/15")));
        }

        @Test
        @DisplayName("Should handle API error response")
        void shouldHandleApiErrorResponse() {
            // Given
            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/history/JPY/2024/1/15"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withBody("Not Found")));

            // When & Then
            assertThatThrownBy(() -> httpClient.fetchByDate(TEST_API_KEY, 2024, 1, 15))
                    .isInstanceOf(RestClientException.class);
        }
    }
}
