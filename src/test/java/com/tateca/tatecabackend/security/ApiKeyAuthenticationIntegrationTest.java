package com.tateca.tatecabackend.security;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.entity.CurrencyNameEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.CurrencyNameRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("API Key Authentication Integration Tests")
class ApiKeyAuthenticationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CurrencyNameRepository currencyNameRepository;

    @Value("${lambda.api.key}")
    private String validApiKey;

    private static final String ENDPOINT = "/internal/exchange-rates";
    private static final String TEST_API_KEY = "test-exchange-rate-api-key";

    @BeforeEach
    void setUp() {
        WireMock.configureFor(wireMock.getHost(), wireMock.getPort());
        WireMock.reset();

        // Setup currency data in database
        List<CurrencyNameEntity> currencies = List.of(
                TestFixtures.Currencies.jpy(),
                TestFixtures.Currencies.usd(),
                TestFixtures.Currencies.eur()
        );
        currencyNameRepository.saveAll(currencies);
        flushAndClear();
    }

    /**
     * Stubs WireMock to return valid exchange rate data
     */
    private void givenExternalApiReturnsValidLatestRates() {
        stubFor(get(urlEqualTo(String.format("/%s/latest/JPY", TEST_API_KEY)))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "result": "success",
                                  "time_last_update_unix": "1704067200",
                                  "conversion_rates": {
                                    "JPY": 1.0,
                                    "USD": 0.0067,
                                    "EUR": 0.0061
                                  }
                                }
                                """)));
    }

    @Nested
    @DisplayName("Valid API Key Authentication")
    class ValidApiKeyTests {

        @Test
        @DisplayName("Should authenticate /internal/exchange-rates with valid X-API-Key")
        void shouldAuthenticateInternalEndpointWithValidApiKey() {
            // Given: Real authentication with X-API-Key, real service, WireMock for external API
            givenExternalApiReturnsValidLatestRates();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", validApiKey);

            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // When: Full integration test (authentication -> controller -> service -> repository -> external API)
            ResponseEntity<Void> response = restTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    httpEntity,
                    Void.class
            );

            // Then: Should successfully update exchange rates
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("Should accept X-API-Key for /internal/** paths")
        void shouldAcceptApiKeyForAllInternalPaths() {
            // Given: Real authentication with X-API-Key
            givenExternalApiReturnsValidLatestRates();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", validApiKey);

            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // When: Full integration test
            ResponseEntity<Void> response = restTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    httpEntity,
                    Void.class
            );

            // Then: Should successfully update exchange rates
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Nested
    @DisplayName("Invalid API Key Authentication")
    class InvalidApiKeyTests {

        @Test
        @DisplayName("Should reject /internal/exchange-rates without X-API-Key header")
        void shouldRejectInternalEndpointWithoutApiKey() {
            // Given
            HttpHeaders headers = new HttpHeaders();

            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should reject /internal/exchange-rates with invalid X-API-Key")
        void shouldRejectInternalEndpointWithInvalidApiKey() {
            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", "invalid-api-key-12345");

            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should reject /internal/exchange-rates with empty X-API-Key")
        void shouldRejectInternalEndpointWithEmptyApiKey() {
            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", "");

            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Should reject /internal/** with Firebase Bearer token")
        void shouldRejectInternalEndpointWithBearerToken() {
            // Given
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer fake-firebase-token");

            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // When
            ResponseEntity<String> response = restTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    httpEntity,
                    String.class
            );

            // Then
            // Should return 401 because X-API-Key header is missing
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Public Endpoints")
    class PublicEndpointsTests {

        @Test
        @DisplayName("Should allow access to /actuator/health without authentication")
        void shouldAllowAccessToActuatorHealth() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/actuator/health",
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        @Test
        @DisplayName("Should allow access to /v3/api-docs without authentication")
        void shouldAllowAccessToApiDocs() {
            // When
            ResponseEntity<String> response = restTemplate.getForEntity(
                    "/v3/api-docs",
                    String.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }
}
