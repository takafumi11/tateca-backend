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

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExchangeRateApiClient Integration Tests")
class ExchangeRateApiClientIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExchangeRateApiClient apiClient;

    private static final String TEST_API_KEY = "test-exchange-rate-api-key";

    @BeforeEach
    void setUp() {
        WireMock.configureFor(wireMock.getHost(), wireMock.getPort());
        WireMock.reset();
    }

    @Nested
    @DisplayName("fetchLatestExchangeRate")
    class FetchLatestExchangeRateTests {

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
            ExchangeRateClientResponse response = apiClient.fetchLatestExchangeRate();

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
        @DisplayName("Should retry on server error and eventually succeed")
        void shouldRetryOnServerErrorAndEventuallySucceed() {
            // Given
            String responseBody = """
                {
                    "result": "success",
                    "time_last_update_unix": "1704067200",
                    "conversion_rates": {
                        "JPY": 1.0
                    }
                }
                """;

            // First two calls fail, third succeeds
            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/latest/JPY"))
                    .inScenario("Retry Scenario")
                    .whenScenarioStateIs("Started")
                    .willReturn(aResponse().withStatus(500))
                    .willSetStateTo("First Failure"));

            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/latest/JPY"))
                    .inScenario("Retry Scenario")
                    .whenScenarioStateIs("First Failure")
                    .willReturn(aResponse().withStatus(500))
                    .willSetStateTo("Second Failure"));

            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/latest/JPY"))
                    .inScenario("Retry Scenario")
                    .whenScenarioStateIs("Second Failure")
                    .willReturn(aResponse()
                            .withStatus(200)
                            .withHeader("Content-Type", "application/json")
                            .withBody(responseBody)));

            // When
            ExchangeRateClientResponse response = apiClient.fetchLatestExchangeRate();

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getResult()).isEqualTo("success");
        }

        @Test
        @DisplayName("Should throw exception after max retries on persistent failure")
        void shouldThrowExceptionAfterMaxRetriesOnPersistentFailure() {
            // Given
            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/latest/JPY"))
                    .willReturn(aResponse().withStatus(500)));

            // When & Then
            assertThatThrownBy(() -> apiClient.fetchLatestExchangeRate())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Exchange rate service unavailable");
        }
    }

    @Nested
    @DisplayName("fetchExchangeRateByDate")
    class FetchExchangeRateByDateTests {

        @Test
        @DisplayName("Should fetch exchange rate for specific date from API")
        void shouldFetchExchangeRateByDateFromApi() {
            // Given
            LocalDate date = LocalDate.of(2024, 1, 15);
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
            ExchangeRateClientResponse response = apiClient.fetchExchangeRateByDate(date);

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
            LocalDate date = LocalDate.of(2024, 1, 15);

            stubFor(get(urlEqualTo("/" + TEST_API_KEY + "/history/JPY/2024/1/15"))
                    .willReturn(aResponse()
                            .withStatus(404)
                            .withBody("Not Found")));

            // When & Then
            assertThatThrownBy(() -> apiClient.fetchExchangeRateByDate(date))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Exchange rate service unavailable");
        }
    }
}
