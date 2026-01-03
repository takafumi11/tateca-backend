package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.service.impl.ExchangeRateServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateService Unit Tests")
class ExchangeRateServiceUnitTest {

    @Mock
    private ExchangeRateAccessor accessor;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    @Test
    @DisplayName("Should call accessor with correct date and return converted DTO")
    void shouldCallAccessorWithCorrectDateAndReturnConvertedDTO() {
        // Given: Accessor returns exchange rate entities
        LocalDate testDate = LocalDate.now();
        List<ExchangeRateEntity> entities = List.of(
                createExchangeRateEntity(TestFixtures.Currencies.usd(), testDate, new BigDecimal("150.25"))
        );

        when(accessor.findAllActiveByDate(testDate)).thenReturn(entities);

        // When: Getting exchange rate
        ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(testDate);

        // Then: Should call accessor with correct date
        verify(accessor, times(1)).findAllActiveByDate(testDate);

        // And: Should return non-null DTO
        assertThat(result).isNotNull();
        assertThat(result.exchangeRateResponseResponseList()).isNotEmpty();
    }

    @Test
    @DisplayName("Should return empty DTO when accessor returns empty list")
    void shouldReturnEmptyDTOWhenAccessorReturnsEmptyList() {
        // Given: Accessor returns empty list
        LocalDate testDate = LocalDate.now();

        when(accessor.findAllActiveByDate(testDate)).thenReturn(Collections.emptyList());

        // When: Getting exchange rate
        ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(testDate);

        // Then: Should return DTO with empty list
        assertThat(result).isNotNull();
        assertThat(result.exchangeRateResponseResponseList()).isEmpty();

        verify(accessor, times(1)).findAllActiveByDate(testDate);
    }

    @Test
    @DisplayName("Should propagate exceptions from accessor layer")
    void shouldPropagateExceptionsFromAccessorLayer() {
        // Given: Accessor throws ResponseStatusException
        LocalDate testDate = LocalDate.now();

        when(accessor.findAllActiveByDate(testDate))
                .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

        // When & Then: Should propagate exception without modification
        assertThatThrownBy(() -> exchangeRateService.getExchangeRate(testDate))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Database error");

        verify(accessor, times(1)).findAllActiveByDate(testDate);
    }

    private ExchangeRateEntity createExchangeRateEntity(CurrencyEntity currency, LocalDate date, BigDecimal rate) {
        return ExchangeRateEntity.builder()
                .currencyCode(currency.getCurrencyCode())
                .currency(currency)
                .date(date)
                .exchangeRate(rate)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isNew(false)
                .build();
    }
}
