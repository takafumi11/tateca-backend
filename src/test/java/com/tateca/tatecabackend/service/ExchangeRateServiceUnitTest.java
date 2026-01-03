package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import com.tateca.tatecabackend.service.impl.ExchangeRateServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

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
@DisplayName("ExchangeRateQueryService Unit Tests")
class ExchangeRateServiceUnitTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateQueryService;

    // ===== Tests for getExchangeRate() =====

    @Test
    @DisplayName("Should call repository with correct date and return converted DTO")
    void shouldCallRepositoryWithCorrectDateAndReturnConvertedDTO() {
        // Given: Repository returns exchange rate entities
        LocalDate testDate = LocalDate.now();
        List<ExchangeRateEntity> entities = List.of(
                createExchangeRateEntity(TestFixtures.Currencies.usd(), testDate, new BigDecimal("150.25"))
        );

        when(exchangeRateRepository.findAllActiveByDate(testDate)).thenReturn(entities);

        // When: Getting exchange rate
        ExchangeRateResponseDTO result = exchangeRateQueryService.getExchangeRate(testDate);

        // Then: Should call repository with correct date
        verify(exchangeRateRepository, times(1)).findAllActiveByDate(testDate);

        // And: Should return non-null DTO
        assertThat(result).isNotNull();
        assertThat(result.exchangeRateResponseResponseList()).isNotEmpty();
    }

    @Test
    @DisplayName("Should return empty DTO when repository returns empty list")
    void shouldReturnEmptyDTOWhenRepositoryReturnsEmptyList() {
        // Given: Repository returns empty list
        LocalDate testDate = LocalDate.now();

        when(exchangeRateRepository.findAllActiveByDate(testDate)).thenReturn(Collections.emptyList());

        // When: Getting exchange rate
        ExchangeRateResponseDTO result = exchangeRateQueryService.getExchangeRate(testDate);

        // Then: Should return DTO with empty list
        assertThat(result).isNotNull();
        assertThat(result.exchangeRateResponseResponseList()).isEmpty();

        verify(exchangeRateRepository, times(1)).findAllActiveByDate(testDate);
    }

    @Test
    @DisplayName("Should propagate DataAccessException from repository layer")
    void shouldPropagateDataAccessExceptionFromRepositoryLayer() {
        // Given: Repository throws DataAccessException
        LocalDate testDate = LocalDate.now();

        when(exchangeRateRepository.findAllActiveByDate(testDate))
                .thenThrow(new DataAccessException("Database connection error") {});

        // When & Then: Should propagate exception without modification
        assertThatThrownBy(() -> exchangeRateQueryService.getExchangeRate(testDate))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Database connection error");

        verify(exchangeRateRepository, times(1)).findAllActiveByDate(testDate);
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
