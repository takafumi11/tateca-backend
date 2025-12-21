package com.tateca.tatecabackend.controller;

import com.tateca.tatecabackend.dto.response.ExchangeRateListResponseDTO;
import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;
import com.tateca.tatecabackend.model.SymbolPosition;
import com.tateca.tatecabackend.service.ExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExchangeRateController.class)
@DisplayName("ExchangeRateController Unit Tests")
class ExchangeRateControllerUnitTest extends AbstractControllerWebTest {

    @MockitoBean
    private ExchangeRateService service;

    @BeforeEach
    void setUp() {
        Mockito.reset(service);
    }

    @Nested
    @DisplayName("GET /exchange-rate/{date}")
    class GetExchangeRateTests {

        @Test
        @DisplayName("Should return 200 OK with exchange rate list")
        void shouldReturnOkWithExchangeRateList() throws Exception {
            // Given
            LocalDate date = LocalDate.of(2024, 1, 1);
            ExchangeRateListResponseDTO responseDTO = createTestResponseDTO();

            when(service.getExchangeRate(date)).thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "2024-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exchange_rate").isArray())
                    .andExpect(jsonPath("$.exchange_rate.length()").value(1))
                    .andExpect(jsonPath("$.exchange_rate[0].currency_code").value("JPY"))
                    .andExpect(jsonPath("$.exchange_rate[0].jp_currency_name").value("日本円"))
                    .andExpect(jsonPath("$.exchange_rate[0].eng_currency_name").value("Japanese Yen"))
                    .andExpect(jsonPath("$.exchange_rate[0].jp_country_name").value("日本"))
                    .andExpect(jsonPath("$.exchange_rate[0].eng_country_name").value("Japan"))
                    .andExpect(jsonPath("$.exchange_rate[0].currency_symbol").value("¥"))
                    .andExpect(jsonPath("$.exchange_rate[0].symbol_position").value("PREFIX"))
                    .andExpect(jsonPath("$.exchange_rate[0].exchange_rate").value("1.0"));

            verify(service).getExchangeRate(date);
        }

        @Test
        @DisplayName("Should return 200 OK with multiple exchange rates")
        void shouldReturnOkWithMultipleExchangeRates() throws Exception {
            // Given
            LocalDate date = LocalDate.of(2024, 1, 1);
            ExchangeRateListResponseDTO responseDTO = createMultipleRatesResponseDTO();

            when(service.getExchangeRate(date)).thenReturn(responseDTO);

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "2024-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exchange_rate").isArray())
                    .andExpect(jsonPath("$.exchange_rate.length()").value(2))
                    .andExpect(jsonPath("$.exchange_rate[0].currency_code").value("JPY"))
                    .andExpect(jsonPath("$.exchange_rate[1].currency_code").value("USD"));

            verify(service).getExchangeRate(date);
        }

        @Test
        @DisplayName("Should return 200 OK with empty list when no rates found")
        void shouldReturnOkWithEmptyList() throws Exception {
            // Given
            LocalDate date = LocalDate.of(2024, 1, 1);
            ExchangeRateListResponseDTO emptyResponse = new ExchangeRateListResponseDTO(List.of());

            when(service.getExchangeRate(date)).thenReturn(emptyResponse);

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "2024-01-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.exchange_rate").isEmpty());

            verify(service).getExchangeRate(date);
        }

        @Test
        @DisplayName("Should return 500 when service throws INTERNAL_SERVER_ERROR")
        void shouldReturn500WhenServiceThrowsInternalError() throws Exception {
            // Given
            LocalDate date = LocalDate.of(2024, 1, 1);

            when(service.getExchangeRate(date))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "2024-01-01"))
                    .andExpect(status().isInternalServerError());

            verify(service).getExchangeRate(date);
        }

        @Test
        @DisplayName("Should return 400 when date format is invalid text")
        void shouldReturn400WhenDateFormatInvalid() throws Exception {
            // When & Then
            mockMvc.perform(get("/exchange-rate/{date}", "invalid-date"))
                    .andExpect(status().isBadRequest());

            verify(service, never()).getExchangeRate(any());
        }

        @Test
        @DisplayName("Should return 400 when date format is wrong pattern")
        void shouldReturn400WhenDateFormatWrongPattern() throws Exception {
            // When & Then - "01-01-2024" is wrong pattern (should be "2024-01-01")
            mockMvc.perform(get("/exchange-rate/{date}", "01-01-2024"))
                    .andExpect(status().isBadRequest());

            verify(service, never()).getExchangeRate(any());
        }

        @Test
        @DisplayName("Should return 400 when date has invalid month")
        void shouldReturn400WhenDateHasInvalidMonth() throws Exception {
            // When & Then - "2024-13-01" is invalid (month 13)
            mockMvc.perform(get("/exchange-rate/{date}", "2024-13-01"))
                    .andExpect(status().isBadRequest());

            verify(service, never()).getExchangeRate(any());
        }

        @Test
        @DisplayName("Should return 400 when date has invalid day")
        void shouldReturn400WhenDateHasInvalidDay() throws Exception {
            // When & Then - "2024-02-30" is invalid (Feb 30)
            mockMvc.perform(get("/exchange-rate/{date}", "2024-02-30"))
                    .andExpect(status().isBadRequest());

            verify(service, never()).getExchangeRate(any());
        }

        @Test
        @DisplayName("Should accept valid leap year date")
        void shouldAcceptValidLeapYearDate() throws Exception {
            // Given
            LocalDate date = LocalDate.of(2024, 2, 29);
            ExchangeRateListResponseDTO responseDTO = createTestResponseDTO();

            when(service.getExchangeRate(date)).thenReturn(responseDTO);

            // When & Then - 2024-02-29 is valid (leap year)
            mockMvc.perform(get("/exchange-rate/{date}", "2024-02-29"))
                    .andExpect(status().isOk());

            verify(service).getExchangeRate(date);
        }
    }

    private ExchangeRateListResponseDTO createTestResponseDTO() {
        ExchangeRateResponseDTO jpyRate = ExchangeRateResponseDTO.builder()
                .currencyCode("JPY")
                .jpCurrencyName("日本円")
                .engCurrencyName("Japanese Yen")
                .jpCountryName("日本")
                .engCountryName("Japan")
                .currencySymbol("¥")
                .symbolPosition(SymbolPosition.PREFIX)
                .exchangeRate("1.0")
                .build();

        return new ExchangeRateListResponseDTO(List.of(jpyRate));
    }

    private ExchangeRateListResponseDTO createMultipleRatesResponseDTO() {
        ExchangeRateResponseDTO jpyRate = ExchangeRateResponseDTO.builder()
                .currencyCode("JPY")
                .jpCurrencyName("日本円")
                .engCurrencyName("Japanese Yen")
                .jpCountryName("日本")
                .engCountryName("Japan")
                .currencySymbol("¥")
                .symbolPosition(SymbolPosition.PREFIX)
                .exchangeRate("1.0")
                .build();

        ExchangeRateResponseDTO usdRate = ExchangeRateResponseDTO.builder()
                .currencyCode("USD")
                .jpCurrencyName("米ドル")
                .engCurrencyName("US Dollar")
                .jpCountryName("アメリカ")
                .engCountryName("United States")
                .currencySymbol("$")
                .symbolPosition(SymbolPosition.PREFIX)
                .exchangeRate("0.0067")
                .build();

        return new ExchangeRateListResponseDTO(List.of(jpyRate, usdRate));
    }
}
