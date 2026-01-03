package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.config.TestSecurityConfig;
import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;
import com.tateca.tatecabackend.dto.response.internal.ExchangeRateResponse;
import com.tateca.tatecabackend.exception.GlobalExceptionHandler;
import com.tateca.tatecabackend.model.SymbolPosition;
import com.tateca.tatecabackend.service.ExchangeRateService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeRateController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
@ActiveProfiles("test")
@DisplayName("ExchangeRateController Web Tests")
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExchangeRateService exchangeRateService;

    private static final String BASE_ENDPOINT = "/exchange-rate";

    @Test
    @DisplayName("Should return 200 OK with exchange rates when date is valid")
    void shouldReturn200WhenDateIsValid() throws Exception {
        // Given: Valid date and service returns exchange rates
        LocalDate testDate = LocalDate.now();
        ExchangeRateResponseDTO expectedResponse = new ExchangeRateResponseDTO(
                List.of(
                        createExchangeRateResponse("USD", "米ドル", "US Dollar", "アメリカ合衆国", "United States", "$", SymbolPosition.PREFIX, "150.25"),
                        createExchangeRateResponse("EUR", "ユーロ", "Euro", "欧州連合", "European Union", "€", SymbolPosition.PREFIX, "165.50"),
                        createExchangeRateResponse("GBP", "英ポンド", "British Pound", "イギリス", "United Kingdom", "£", SymbolPosition.PREFIX, "190.75")
                )
        );

        when(exchangeRateService.getExchangeRate(testDate))
                .thenReturn(expectedResponse);

        // When & Then: Should return 200 with exchange rates
        mockMvc.perform(get(BASE_ENDPOINT + "/{date}", testDate))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.exchange_rate").isArray())
                .andExpect(jsonPath("$.exchange_rate.length()").value(3))
                .andExpect(jsonPath("$.exchange_rate[0].currency_code").value("USD"))
                .andExpect(jsonPath("$.exchange_rate[0].exchange_rate").value(150.25))
                .andExpect(jsonPath("$.exchange_rate[1].currency_code").value("EUR"))
                .andExpect(jsonPath("$.exchange_rate[1].exchange_rate").value(165.50))
                .andExpect(jsonPath("$.exchange_rate[2].currency_code").value("GBP"))
                .andExpect(jsonPath("$.exchange_rate[2].exchange_rate").value(190.75));

        // And: Service should be called once
        verify(exchangeRateService, times(1)).getExchangeRate(testDate);
    }

    @Test
    @DisplayName("Should return 200 OK with empty list when no rates found")
    void shouldReturn200WithEmptyListWhenNoRatesFound() throws Exception {
        // Given: Valid date but no exchange rates found
        LocalDate testDate = LocalDate.now();
        ExchangeRateResponseDTO expectedResponse = new ExchangeRateResponseDTO(List.of());

        when(exchangeRateService.getExchangeRate(testDate))
                .thenReturn(expectedResponse);

        // When & Then: Should return 200 with empty array
        mockMvc.perform(get(BASE_ENDPOINT + "/{date}", testDate))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.exchange_rate").isArray())
                .andExpect(jsonPath("$.exchange_rate.length()").value(0));

        verify(exchangeRateService, times(1)).getExchangeRate(testDate);
    }

    @Test
    @DisplayName("Should return 400 when date format is invalid")
    void shouldReturn400WhenDateFormatIsInvalid() throws Exception {
        // Given: Invalid date format
        String invalidDate = "2024-13-45";

        // When & Then: Should return 400
        mockMvc.perform(get(BASE_ENDPOINT + "/{date}", invalidDate))
                .andExpect(status().isBadRequest());

        // And: Service should not be called
        verify(exchangeRateService, never()).getExchangeRate(any());
    }

    @Test
    @DisplayName("Should return 400 when date is malformed string")
    void shouldReturn400WhenDateIsMalformedString() throws Exception {
        // Given: Malformed date string
        String malformedDate = "not-a-date";

        // When & Then: Should return 400
        mockMvc.perform(get(BASE_ENDPOINT + "/{date}", malformedDate))
                .andExpect(status().isBadRequest());

        // And: Service should not be called
        verify(exchangeRateService, never()).getExchangeRate(any());
    }

    @Test
    @DisplayName("Should return 500 when service throws internal server error")
    void shouldReturn500WhenServiceThrowsInternalServerError() throws Exception {
        // Given: Service throws DataAccessException
        LocalDate testDate = LocalDate.now();

        when(exchangeRateService.getExchangeRate(testDate))
                .thenThrow(new DataAccessException("Database connection error") {});

        // When & Then: Should return 500
        mockMvc.perform(get(BASE_ENDPOINT + "/{date}", testDate))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").exists());

        verify(exchangeRateService, times(1)).getExchangeRate(testDate);
    }

    @Test
    @DisplayName("Should accept date with past year")
    void shouldAcceptDateWithPastYear() throws Exception {
        // Given: Date from past (5 years ago)
        LocalDate pastDate = LocalDate.now().minusYears(5);
        ExchangeRateResponseDTO expectedResponse = new ExchangeRateResponseDTO(
                List.of(createExchangeRateResponse("USD", "米ドル", "US Dollar", "アメリカ合衆国", "United States", "$", SymbolPosition.PREFIX, "107.50"))
        );

        when(exchangeRateService.getExchangeRate(pastDate))
                .thenReturn(expectedResponse);

        // When & Then: Should accept past date
        mockMvc.perform(get(BASE_ENDPOINT + "/{date}", pastDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchange_rate[0].currency_code").value("USD"));

        verify(exchangeRateService, times(1)).getExchangeRate(pastDate);
    }

    @Test
    @DisplayName("Should accept date with future year")
    void shouldAcceptDateWithFutureYear() throws Exception {
        // Given: Date from future (5 years from now)
        LocalDate futureDate = LocalDate.now().plusYears(5);
        ExchangeRateResponseDTO expectedResponse = new ExchangeRateResponseDTO(
                List.of(createExchangeRateResponse("EUR", "ユーロ", "Euro", "欧州連合", "European Union", "€", SymbolPosition.PREFIX, "180.00"))
        );

        when(exchangeRateService.getExchangeRate(futureDate))
                .thenReturn(expectedResponse);

        // When & Then: Should accept future date
        mockMvc.perform(get(BASE_ENDPOINT + "/{date}", futureDate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exchange_rate[0].currency_code").value("EUR"));

        verify(exchangeRateService, times(1)).getExchangeRate(futureDate);
    }

    private ExchangeRateResponse createExchangeRateResponse(
            String currencyCode,
            String jpCurrencyName,
            String engCurrencyName,
            String jpCountryName,
            String engCountryName,
            String currencySymbol,
            SymbolPosition symbolPosition,
            String exchangeRate
    ) {
        return new ExchangeRateResponse(
                currencyCode,
                jpCurrencyName,
                engCurrencyName,
                jpCountryName,
                engCountryName,
                currencySymbol,
                symbolPosition,
                exchangeRate
        );
    }
}
