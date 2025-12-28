package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
import com.tateca.tatecabackend.service.ExchangeRateUpdateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

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

    @MockitoBean
    private ExchangeRateUpdateService exchangeRateUpdateService;

    private static final String ENDPOINT = "/internal/exchange-rates";

    @Test
    @DisplayName("Should return 204 No Content when update succeeds")
    void shouldReturn204WhenUpdateSucceeds() throws Exception {
        // Given: Service successfully updates rates (3 currencies × 2 dates = 6 records)
        when(exchangeRateUpdateService.fetchAndStoreLatestExchangeRate()).thenReturn(6);

        // When: Calling endpoint without request body
        mockMvc.perform(post(ENDPOINT))
                .andExpect(status().isNoContent());

        // Then: Service should be called once
        verify(exchangeRateUpdateService, times(1)).fetchAndStoreLatestExchangeRate();
    }

    @Test
    @DisplayName("Should propagate service exceptions to GlobalExceptionHandler")
    void shouldPropagateServiceExceptions() throws Exception {
        // Given: Service throws ResponseStatusException
        when(exchangeRateUpdateService.fetchAndStoreLatestExchangeRate())
                .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "API unavailable"));

        // When: Calling endpoint
        // Then: Should return 500 (handled by GlobalExceptionHandler)
        mockMvc.perform(post(ENDPOINT))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").exists());

        // And: Service should be called
        verify(exchangeRateUpdateService, times(1)).fetchAndStoreLatestExchangeRate();
    }

    @Test
    @DisplayName("Should update latest exchange rates without parameters")
    void shouldUpdateLatestExchangeRatesWithoutParameters() throws Exception {
        // Given: Service successfully fetches and stores latest rates (for today and tomorrow)
        when(exchangeRateUpdateService.fetchAndStoreLatestExchangeRate()).thenReturn(6);

        // When: Calling endpoint
        mockMvc.perform(post(ENDPOINT))
                .andExpect(status().isNoContent());

        // Then: Service should fetch latest rates (no date parameter)
        verify(exchangeRateUpdateService, times(1)).fetchAndStoreLatestExchangeRate();
    }
}
