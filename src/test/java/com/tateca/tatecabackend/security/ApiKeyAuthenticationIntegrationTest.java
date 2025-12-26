package com.tateca.tatecabackend.security;

import com.tateca.tatecabackend.AbstractIntegrationTest;
import com.tateca.tatecabackend.dto.request.ExchangeRateUpdateRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.tateca.tatecabackend.service.ExchangeRateUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("API Key Authentication Integration Tests")
class ApiKeyAuthenticationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private ExchangeRateUpdateService exchangeRateUpdateService;

    @Value("${lambda.api.key}")
    private String validApiKey;

    private static final String ENDPOINT = "/internal/exchange-rates";

    @Nested
    @DisplayName("Valid API Key Authentication")
    class ValidApiKeyTests {

        @Test
        @DisplayName("Should authenticate /internal/exchange-rates with valid X-API-Key")
        void shouldAuthenticateInternalEndpointWithValidApiKey() {
            // Given
            when(exchangeRateUpdateService.fetchAndStoreExchangeRateByDate(any(LocalDate.class)))
                    .thenReturn(3);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", validApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ExchangeRateUpdateRequestDTO request =
                    new ExchangeRateUpdateRequestDTO(LocalDate.now());

            HttpEntity<ExchangeRateUpdateRequestDTO> httpEntity =
                    new HttpEntity<>(request, headers);

            // When
            ResponseEntity<Void> response = restTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    httpEntity,
                    Void.class
            );

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("Should accept X-API-Key for /internal/** paths")
        void shouldAcceptApiKeyForAllInternalPaths() {
            // Given
            when(exchangeRateUpdateService.fetchAndStoreExchangeRateByDate(any(LocalDate.class)))
                    .thenReturn(3);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-API-Key", validApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ExchangeRateUpdateRequestDTO request =
                    new ExchangeRateUpdateRequestDTO(LocalDate.of(2024, 1, 1));

            HttpEntity<ExchangeRateUpdateRequestDTO> httpEntity =
                    new HttpEntity<>(request, headers);

            // When
            ResponseEntity<Void> response = restTemplate.exchange(
                    ENDPOINT,
                    HttpMethod.POST,
                    httpEntity,
                    Void.class
            );

            // Then
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
            headers.setContentType(MediaType.APPLICATION_JSON);

            ExchangeRateUpdateRequestDTO request =
                    new ExchangeRateUpdateRequestDTO(LocalDate.now());

            HttpEntity<ExchangeRateUpdateRequestDTO> httpEntity =
                    new HttpEntity<>(request, headers);

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
            headers.setContentType(MediaType.APPLICATION_JSON);

            ExchangeRateUpdateRequestDTO request =
                    new ExchangeRateUpdateRequestDTO(LocalDate.now());

            HttpEntity<ExchangeRateUpdateRequestDTO> httpEntity =
                    new HttpEntity<>(request, headers);

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
            headers.setContentType(MediaType.APPLICATION_JSON);

            ExchangeRateUpdateRequestDTO request =
                    new ExchangeRateUpdateRequestDTO(LocalDate.now());

            HttpEntity<ExchangeRateUpdateRequestDTO> httpEntity =
                    new HttpEntity<>(request, headers);

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
            headers.setContentType(MediaType.APPLICATION_JSON);

            ExchangeRateUpdateRequestDTO request =
                    new ExchangeRateUpdateRequestDTO(LocalDate.now());

            HttpEntity<ExchangeRateUpdateRequestDTO> httpEntity =
                    new HttpEntity<>(request, headers);

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
