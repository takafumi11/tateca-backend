package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.dto.response.ExchangeRateResponseDTO;
import com.tateca.tatecabackend.entity.CurrencyEntity;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.repository.ExchangeRateRepository;
import com.tateca.tatecabackend.service.impl.ExchangeRateServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExchangeRateServiceImpl — Domain Logic")
class ExchangeRateServiceUnitTest {

    @Mock
    private ExchangeRateRepository exchangeRateRepository;

    @InjectMocks
    private ExchangeRateServiceImpl exchangeRateService;

    @Nested
    @DisplayName("Given 為替レート取得")
    class GetExchangeRate {

        @Test
        @DisplayName("Then Repository に正しい日付を渡し、変換済み DTO を返却する")
        void shouldCallRepositoryWithCorrectDateAndReturnConvertedDTO() {
            // Given
            LocalDate testDate = LocalDate.now();
            List<ExchangeRateEntity> entities = List.of(
                    createExchangeRateEntity(TestFixtures.Currencies.usd(), testDate, new BigDecimal("150.25"))
            );
            when(exchangeRateRepository.findAllActiveByDate(testDate)).thenReturn(entities);

            // When
            ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(testDate);

            // Then
            verify(exchangeRateRepository).findAllActiveByDate(testDate);
            assertThat(result).isNotNull();
            assertThat(result.exchangeRateResponseResponseList()).isNotEmpty();
        }

        @Test
        @DisplayName("Then レートが存在しない場合は空の DTO を返却する")
        void shouldReturnEmptyDTOWhenRepositoryReturnsEmptyList() {
            // Given
            LocalDate testDate = LocalDate.now();
            when(exchangeRateRepository.findAllActiveByDate(testDate)).thenReturn(Collections.emptyList());

            // When
            ExchangeRateResponseDTO result = exchangeRateService.getExchangeRate(testDate);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.exchangeRateResponseResponseList()).isEmpty();
            verify(exchangeRateRepository).findAllActiveByDate(testDate);
        }

        @Test
        @DisplayName("Then Repository の例外はそのまま伝播する")
        void shouldPropagateDataAccessExceptionFromRepositoryLayer() {
            // Given
            LocalDate testDate = LocalDate.now();
            when(exchangeRateRepository.findAllActiveByDate(testDate))
                    .thenThrow(new DataAccessException("Database connection error") {});

            // When & Then
            assertThatThrownBy(() -> exchangeRateService.getExchangeRate(testDate))
                    .isInstanceOf(DataAccessException.class)
                    .hasMessageContaining("Database connection error");
            verify(exchangeRateRepository).findAllActiveByDate(testDate);
        }
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
