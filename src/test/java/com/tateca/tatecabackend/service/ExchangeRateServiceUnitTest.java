package com.tateca.tatecabackend.service;

import com.tateca.tatecabackend.accessor.ExchangeRateAccessor;
import com.tateca.tatecabackend.dto.response.ExchangeRateListResponseDTO;
import com.tateca.tatecabackend.entity.ExchangeRateEntity;
import com.tateca.tatecabackend.fixtures.TestFixtures;
import com.tateca.tatecabackend.service.impl.ExchangeRateServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ExchangeRateService Unit Tests")
class ExchangeRateServiceUnitTest extends AbstractServiceUnitTest {

    @Mock
    private ExchangeRateAccessor accessor;

    @InjectMocks
    private ExchangeRateServiceImpl service;

    private LocalDate testDate;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.of(2024, 1, 1);
    }

    @Nested
    @DisplayName("getExchangeRate")
    class GetExchangeRateTests {

        @Test
        @DisplayName("Should return exchange rate list DTO for given date")
        void shouldReturnExchangeRateListDTO() {
            // Given
            List<ExchangeRateEntity> entities = List.of(
                    TestFixtures.ExchangeRates.jpy(testDate),
                    TestFixtures.ExchangeRates.usd(testDate, new BigDecimal("0.0067"))
            );
            when(accessor.findAllActiveByDate(testDate)).thenReturn(entities);

            // When
            ExchangeRateListResponseDTO result = service.getExchangeRate(testDate);

            // Then
            assertThat(result.getExchangeRateResponseList()).hasSize(2);
            assertThat(result.getExchangeRateResponseList().get(0).getCurrencyCode()).isEqualTo("JPY");
            assertThat(result.getExchangeRateResponseList().get(1).getCurrencyCode()).isEqualTo("USD");
            verify(accessor).findAllActiveByDate(testDate);
        }

        @Test
        @DisplayName("Should return empty list DTO when no rates found")
        void shouldReturnEmptyListDTO() {
            // Given
            when(accessor.findAllActiveByDate(testDate)).thenReturn(List.of());

            // When
            ExchangeRateListResponseDTO result = service.getExchangeRate(testDate);

            // Then
            assertThat(result.getExchangeRateResponseList()).isEmpty();
            verify(accessor).findAllActiveByDate(testDate);
        }

        @Test
        @DisplayName("Should propagate exception from accessor")
        void shouldPropagateExceptionFromAccessor() {
            // Given
            when(accessor.findAllActiveByDate(testDate))
                    .thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Database error"));

            // When & Then
            assertThatThrownBy(() -> service.getExchangeRate(testDate))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(ex -> {
                        ResponseStatusException rse = (ResponseStatusException) ex;
                        assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        }

        @Test
        @DisplayName("Should correctly map exchange rates in returned DTO")
        void shouldCorrectlyMapExchangeRates() {
            // Given
            List<ExchangeRateEntity> entities = List.of(
                    TestFixtures.ExchangeRates.jpy(testDate),
                    TestFixtures.ExchangeRates.usd(testDate, new BigDecimal("0.0067")),
                    TestFixtures.ExchangeRates.eur(testDate, new BigDecimal("0.0061"))
            );
            when(accessor.findAllActiveByDate(testDate)).thenReturn(entities);

            // When
            ExchangeRateListResponseDTO result = service.getExchangeRate(testDate);

            // Then
            assertThat(result.getExchangeRateResponseList()).hasSize(3);
            assertThat(result.getExchangeRateResponseList().get(0).getExchangeRate()).isEqualTo("1");
            assertThat(result.getExchangeRateResponseList().get(1).getExchangeRate()).isEqualTo("0.0067");
            assertThat(result.getExchangeRateResponseList().get(2).getExchangeRate()).isEqualTo("0.0061");
        }
    }
}
