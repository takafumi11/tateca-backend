package com.tateca.tatecabackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.dto.request.ExchangeRateUpdateRequestDTO;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
import com.tateca.tatecabackend.service.ExchangeRateUpdateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeRateLambdaController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("ExchangeRateLambdaController Web Tests")
class ExchangeRateLambdaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ExchangeRateUpdateService exchangeRateUpdateService;

    private static final String ENDPOINT = "/internal/exchange-rates";

    /**
     * Converts an object to JSON string for request bodies.
     */
    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    @Test
    @DisplayName("Should return 204 No Content when update succeeds")
    void shouldReturn204WhenUpdateSucceeds() throws Exception {
        // Given: Service successfully updates rates
        LocalDate testDate = LocalDate.of(2024, 1, 15);
        when(exchangeRateUpdateService.fetchAndStoreExchangeRateByDate(testDate)).thenReturn(3);

        ExchangeRateUpdateRequestDTO request = new ExchangeRateUpdateRequestDTO(testDate);

        // When: Calling endpoint
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isNoContent());

        // Then: Service should be called once
        verify(exchangeRateUpdateService, times(1)).fetchAndStoreExchangeRateByDate(testDate);
    }

    @Test
    @DisplayName("Should return 400 Bad Request when target_date is missing")
    void shouldReturn400WhenTargetDateIsMissing() throws Exception {
        // Given: Request without target_date
        String invalidRequest = "{}";

        // When: Calling endpoint
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        // Then: Service should not be called
        verify(exchangeRateUpdateService, never()).fetchAndStoreExchangeRateByDate(any());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when target_date is null")
    void shouldReturn400WhenTargetDateIsNull() throws Exception {
        // Given: Request with null target_date
        String invalidRequest = "{\"target_date\": null}";

        // When: Calling endpoint
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest());

        // Then: Service should not be called
        verify(exchangeRateUpdateService, never()).fetchAndStoreExchangeRateByDate(any());
    }

    @Test
    @DisplayName("Should return 400 Bad Request when request body is empty")
    void shouldReturn400WhenRequestBodyIsEmpty() throws Exception {
        // When: Calling endpoint without body
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Then: Service should not be called
        verify(exchangeRateUpdateService, never()).fetchAndStoreExchangeRateByDate(any());
    }

    @Test
    @DisplayName("Should propagate service exceptions to GlobalExceptionHandler")
    void shouldPropagateServiceExceptions() throws Exception {
        // Given: Service throws ResponseStatusException
        LocalDate testDate = LocalDate.of(2024, 2, 1);
        when(exchangeRateUpdateService.fetchAndStoreExchangeRateByDate(testDate))
                .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "API unavailable"));

        ExchangeRateUpdateRequestDTO request = new ExchangeRateUpdateRequestDTO(testDate);

        // When: Calling endpoint
        // Then: Should return 500 (handled by GlobalExceptionHandler)
        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").exists());

        // And: Service should be called
        verify(exchangeRateUpdateService, times(1)).fetchAndStoreExchangeRateByDate(testDate);
    }

    @Nested
    @DisplayName("Request Validation Tests")
    class RequestValidationTests {

        @Test
        @DisplayName("Should return 400 Bad Request when target_date format is invalid")
        void shouldReturn400WhenDateFormatInvalid() throws Exception {
            // Given: Invalid date format
            String invalidRequest = "{\"target_date\": \"2024-13-45\"}";

            // When: Calling endpoint
            mockMvc.perform(post(ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidRequest))
                    .andExpect(status().isBadRequest());

            // Then: Service should not be called
            verify(exchangeRateUpdateService, never()).fetchAndStoreExchangeRateByDate(any());
        }

        @Test
        @DisplayName("Should accept past dates for historical data")
        void shouldAcceptPastDates() throws Exception {
            // Given: Past date
            LocalDate pastDate = LocalDate.of(2020, 1, 1);
            when(exchangeRateUpdateService.fetchAndStoreExchangeRateByDate(pastDate))
                    .thenReturn(3);

            ExchangeRateUpdateRequestDTO request = new ExchangeRateUpdateRequestDTO(pastDate);

            // When: Calling endpoint
            mockMvc.perform(post(ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());

            // Then: Service should be called with past date
            verify(exchangeRateUpdateService, times(1)).fetchAndStoreExchangeRateByDate(pastDate);
        }

        @Test
        @DisplayName("Should accept future dates for prediction data")
        void shouldAcceptFutureDates() throws Exception {
            // Given: Future date
            LocalDate futureDate = LocalDate.of(2030, 12, 31);
            when(exchangeRateUpdateService.fetchAndStoreExchangeRateByDate(futureDate))
                    .thenReturn(3);

            ExchangeRateUpdateRequestDTO request = new ExchangeRateUpdateRequestDTO(futureDate);

            // When: Calling endpoint
            mockMvc.perform(post(ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());

            // Then: Service should be called with future date
            verify(exchangeRateUpdateService, times(1)).fetchAndStoreExchangeRateByDate(futureDate);
        }

        @Test
        @DisplayName("Should accept today's date")
        void shouldAcceptTodaysDate() throws Exception {
            // Given: Today's date
            LocalDate today = LocalDate.now();
            when(exchangeRateUpdateService.fetchAndStoreExchangeRateByDate(today))
                    .thenReturn(3);

            ExchangeRateUpdateRequestDTO request = new ExchangeRateUpdateRequestDTO(today);

            // When: Calling endpoint
            mockMvc.perform(post(ENDPOINT)
                                .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(request)))
                    .andExpect(status().isNoContent());

            // Then: Service should be called
            verify(exchangeRateUpdateService, times(1)).fetchAndStoreExchangeRateByDate(today);
        }
    }
}
