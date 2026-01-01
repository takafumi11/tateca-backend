package com.tateca.tatecabackend.api.client;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.api.response.ExchangeRateClientResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ExchangeRateApiClient Integration Tests")
class ExchangeRateResponseApiClientIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ExchangeRateApiClient apiClient;

    private static final String TEST_API_KEY = "test-exchange-rate-api-key";

    @BeforeEach
    void setUp() {
        WireMock.configureFor(wireMock.getHost(), wireMock.getPort());
        WireMock.reset();
    }

    @Nested
    @DisplayName("Given external API is available")
    class WhenExternalApiIsAvailable {

        @Test
        @DisplayName("Then should successfully fetch latest exchange rates")
        void thenShouldSuccessfullyFetchLatestExchangeRates() {
            // Given: External API returns valid response
            givenExternalApiReturnsValidResponse();

            // When: Fetching latest exchange rate
            ExchangeRateClientResponse response = apiClient.fetchLatestExchangeRate();

            // Then: Should receive valid response
            assertThat(response).isNotNull();
            assertThat(response.result()).isEqualTo("success");
            assertThat(response.timeLastUpdateUnix()).isEqualTo("1704067200");
            assertThat(response.conversionRates())
                    .containsEntry("JPY", 1.0)
                    .containsEntry("USD", 0.0067)
                    .containsEntry("EUR", 0.0061);

            verifyApiWasCalledOnce("/latest/JPY");
        }
    }

    @Nested
    @DisplayName("Given external API is temporarily unavailable")
    class WhenExternalApiIsTemporarilyUnavailable {

        @Test
        @DisplayName("Then should retry and eventually succeed")
        void thenShouldRetryAndEventuallySucceed() {
            // Given: External API fails twice then succeeds on third attempt
            givenExternalApiFailsTwiceThenSucceeds();

            // When: Fetching latest exchange rate
            ExchangeRateClientResponse response = apiClient.fetchLatestExchangeRate();

            // Then: Should succeed after retries
            assertThat(response).isNotNull();
            assertThat(response.result()).isEqualTo("success");

            // And: Should have retried 3 times (2 failures + 1 success)
            verifyApiWasCalledThreeTimes("/latest/JPY");
        }

        @Test
        @DisplayName("Then should invoke fallback after max retries")
        void thenShouldInvokeFallbackAfterMaxRetries() {
            // Given: External API always returns 500
            givenExternalApiAlwaysFails("/latest/JPY");

            // When & Then: Should invoke fallback and throw exception
            assertThatThrownBy(() -> apiClient.fetchLatestExchangeRate())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Exchange rate service unavailable");

            // And: Should have attempted 3 times (max retries)
            verifyApiWasCalledThreeTimes("/latest/JPY");
        }
    }

    @Nested
    @DisplayName("Given external API returns error response")
    class WhenExternalApiReturnsErrorResponse {

        @Test
        @DisplayName("Then should handle 404 gracefully for latest endpoint")
        void thenShouldHandle404GracefullyForLatestEndpoint() {
            // Given: External API returns 404
            givenExternalApiReturns404("/latest/JPY");

            // When & Then: Should throw exception with context
            assertThatThrownBy(() -> apiClient.fetchLatestExchangeRate())
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Exchange rate service unavailable");
        }
    }

    // ========== Helper Methods for Test Setup ==========

    private void givenExternalApiReturnsValidResponse() {
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
    }

    private void givenExternalApiFailsTwiceThenSucceeds() {
        String successResponse = """
            {
                "result": "success",
                "time_last_update_unix": "1704067200",
                "conversion_rates": {
                    "JPY": 1.0
                }
            }
            """;

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
                        .withBody(successResponse)));
    }

    private void givenExternalApiAlwaysFails(String path) {
        stubFor(get(urlEqualTo("/" + TEST_API_KEY + path))
                .willReturn(aResponse().withStatus(500)));
    }

    private void givenExternalApiReturns404(String path) {
        stubFor(get(urlEqualTo("/" + TEST_API_KEY + path))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Not Found")));
    }

    private void verifyApiWasCalledOnce(String path) {
        verify(1, getRequestedFor(urlEqualTo("/" + TEST_API_KEY + path)));
    }

    private void verifyApiWasCalledThreeTimes(String path) {
        verify(3, getRequestedFor(urlEqualTo("/" + TEST_API_KEY + path)));
    }
}
